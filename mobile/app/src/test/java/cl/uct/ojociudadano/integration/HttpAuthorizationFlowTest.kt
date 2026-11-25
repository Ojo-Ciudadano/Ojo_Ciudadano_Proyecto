package cl.uct.ojociudadano.integration

import cl.uct.ojociudadano.data.model.RegistrationInput
import cl.uct.ojociudadano.data.remote.RetrofitClient
import cl.uct.ojociudadano.data.remote.error.ApiException
import cl.uct.ojociudadano.data.repository.RemoteAuthRepository
import cl.uct.ojociudadano.data.repository.RemoteCategoryRepository
import cl.uct.ojociudadano.data.session.DefaultSessionManager
import cl.uct.ojociudadano.data.session.SessionExpirationEvents
import cl.uct.ojociudadano.data.session.SessionState
import cl.uct.ojociudadano.data.session.TestSessionStorage
import cl.uct.ojociudadano.data.session.TokenSnapshot
import cl.uct.ojociudadano.data.session.testSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HttpAuthorizationFlowTest {
    @Test
    fun `protected 401 reaches repository and emits snapshot without closing session`() =
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
        val snapshot = manager.currentTokenSnapshot()
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
            assertEquals(SessionState.Authenticated(session), manager.state.value)
            assertEquals(snapshot, manager.currentTokenSnapshot())
            assertEquals(session.accessToken, manager.currentToken())
            assertEquals(session, storage.currentSession())
            assertEquals(1, server.requestCount)
        } finally {
            server.shutdown()
        }
    }

    private enum class Operation { Protected, Login, Register, PublicClient }
}
