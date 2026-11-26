package cl.uct.ojociudadano.integration

import cl.uct.ojociudadano.data.model.RegistrationInput
import cl.uct.ojociudadano.data.remote.RetrofitClient
import cl.uct.ojociudadano.data.remote.error.ApiException
import cl.uct.ojociudadano.data.repository.RemoteAuthRepository
import cl.uct.ojociudadano.data.repository.RemoteCategoryRepository
import cl.uct.ojociudadano.data.session.DefaultSessionManager
import cl.uct.ojociudadano.data.session.SessionExpirationEvents
import cl.uct.ojociudadano.data.session.SessionManager
import cl.uct.ojociudadano.data.session.SessionState
import cl.uct.ojociudadano.data.session.TestSessionStorage
import cl.uct.ojociudadano.data.session.TokenSnapshot
import cl.uct.ojociudadano.data.session.testSession
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HttpAuthorizationFlowTest {
    @Test
    fun `protected 401 reaches repository and clears the session through its expiration event`() =
        checkResponse(401, Operation.Protected, expectedExpiration = true)

    @Test
    fun `login 401 preserves invalid credentials error without expiration`() =
        checkResponse(401, Operation.Login)

    @Test
    fun `public registration 401 does not expire active session`() =
        checkResponse(401, Operation.Register)

    @Test
    fun `protected 403 reaches repository as permissions error and preserves session`() =
        checkResponse(403, Operation.Protected)

    @Test
    fun `public client 401 does not expire active session`() =
        checkResponse(401, Operation.PublicClient)

    @Test
    fun `other http errors retain their translation and do not expire session`() {
        listOf(400, 404, 422, 500, 503).forEach { checkResponse(it, Operation.Protected) }
    }

    @Test
    fun `concurrent protected 401 responses clear persistence once and cannot reopen the notice`() = runTest {
        val storage = TestSessionStorage(testSession())
        val manager = DefaultSessionManager(storage)
        manager.restore()
        val snapshot = requireNotNull(manager.currentTokenSnapshot())
        val processed = Channel<TokenSnapshot>(capacity = 2)
        SessionExpirationEvents.observeIn(backgroundScope, object : SessionManager by manager {
            override fun invalidateIfCurrent(rejectedToken: TokenSnapshot) {
                manager.invalidateIfCurrent(rejectedToken)
                check(processed.trySend(rejectedToken).isSuccess)
            }
        })

        val requestsArrived = CountDownLatch(2)
        MockWebServer().use { server ->
            server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    requestsArrived.countDown()
                    val bothInFlight = requestsArrived.await(5, TimeUnit.SECONDS)
                    return MockResponse().setResponseCode(if (bothInFlight) 401 else 500)
                }
            }
            val repository = RemoteCategoryRepository(
                RetrofitClient.createApiService(server.url("/").toString(), manager)
            )
            val responses = List(2) {
                async {
                    runCatching { repository.getCategories() }.exceptionOrNull() as? ApiException.Http
                }
            }

            assertEquals(listOf(401, 401), responses.map { it.await()?.statusCode })
            val expired = SessionState.Expired(requireNotNull(snapshot.sessionGeneration))
            withContext(Dispatchers.IO) {
                withTimeout(5_000) {
                    assertEquals(snapshot, processed.receive())
                    manager.dismissExpiration(expired)
                    assertEquals(snapshot, processed.receive())
                }
            }
            assertEquals(SessionState.Unauthenticated, manager.state.value)
            assertEquals(1, storage.clearCalls)
            assertNull(manager.currentToken())
            assertNull(storage.currentSession())
        }
    }

    private fun checkResponse(
        code: Int,
        operation: Operation,
        expectedExpiration: Boolean = false
    ) = runTest {
        val events = mutableListOf<TokenSnapshot>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            SessionExpirationEvents.events.collect { events += it }
        }
        val session = testSession()
        val storage = TestSessionStorage(session)
        val manager = DefaultSessionManager(storage, Dispatchers.Unconfined)
        manager.restore()
        val snapshot = requireNotNull(manager.currentTokenSnapshot())
        SessionExpirationEvents.observeIn(backgroundScope, manager)
        val server = MockWebServer()
        // El tipo de error de permisos no depende de un cuerpo JSON válido.
        server.enqueue(MockResponse().setResponseCode(code).setBody("not-json"))
        server.start()
        try {
            val api = RetrofitClient.createApiService(
                baseUrl = server.url("/").toString(),
                tokenProvider = if (operation == Operation.PublicClient) null else manager
            )
            val failure = runCatching {
                when (operation) {
                    Operation.Protected, Operation.PublicClient -> RemoteCategoryRepository(api).getCategories()
                    Operation.Login -> RemoteAuthRepository(api).login("test@example.invalid", "invalid")
                    Operation.Register -> RemoteAuthRepository(api).register(
                        RegistrationInput("Test", "test@example.invalid", "password")
                    )
                }
            }.exceptionOrNull()

            assertTrue(failure is ApiException.Http)
            assertEquals(code, (failure as ApiException.Http).statusCode)
            if (code == 403) {
                assertTrue(failure is ApiException.InsufficientPermissions)
                assertEquals("No tienes permisos suficientes para realizar esta operación", failure.message)
            } else {
                assertFalse(failure is ApiException.InsufficientPermissions)
                assertEquals("Error HTTP $code", failure.message)
            }
            val request = server.takeRequest()
            if (operation == Operation.Protected) {
                assertEquals("Bearer ${session.accessToken}", request.getHeader("Authorization"))
            } else {
                assertNull(request.getHeader("Authorization"))
            }
            assertEquals(if (expectedExpiration) listOf(snapshot) else emptyList<TokenSnapshot>(), events)
            if (expectedExpiration) {
                val expired = SessionState.Expired(requireNotNull(snapshot.sessionGeneration))
                withContext(Dispatchers.IO) {
                    withTimeout(5_000) { manager.state.first { it == expired } }
                }
                assertNull(manager.currentToken())
                assertNull(storage.currentSession())
                assertEquals(1, storage.clearCalls)
            } else {
                assertEquals(SessionState.Authenticated(session), manager.state.value)
                assertEquals(snapshot, manager.currentTokenSnapshot())
                assertEquals(session, storage.currentSession())
            }
            assertEquals(1, server.requestCount)
        } finally {
            server.shutdown()
        }
    }

    private enum class Operation { Protected, Login, Register, PublicClient }
}
