package cl.uct.ojociudadano.data.remote.auth

import cl.uct.ojociudadano.data.session.AuthenticatedSession
import cl.uct.ojociudadano.data.session.DefaultSessionManager
import cl.uct.ojociudadano.data.session.SessionState
import cl.uct.ojociudadano.data.session.SessionStorage
import cl.uct.ojociudadano.data.session.TokenProvider
import cl.uct.ojociudadano.data.session.TokenSnapshot
import java.util.Base64
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BearerTokenInterceptorTest {

    @Test
    fun `adds bearer token to request when a token is available`() {
        executeRequest(token = "jwt-value") { request ->
            assertEquals("Bearer jwt-value", request.getHeader("Authorization"))
        }
    }

    @Test
    fun `does not add authorization header when token is absent or blank`() {
        listOf(null, "", "   ").forEach { token ->
            executeRequest(token = token) { request ->
                assertNull(request.getHeader("Authorization"))
            }
        }
    }

    @Test
    fun `does not overwrite an existing authorization header`() {
        executeRequest(
            token = "session-jwt",
            authorizationHeader = "Custom existing-credential"
        ) { request ->
            assertEquals(
                "Custom existing-credential",
                request.getHeader("Authorization")
            )
        }
    }

    @Test
    fun `401 invalidates the token used by the request without retrying`() {
        val rejectedTokens = mutableListOf<String>()
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(401))
        server.start()

        try {
            val client = client(
                tokenProvider = FakeTokenProvider("rejected-jwt"),
                onUnauthorized = { rejectedTokens += it.value }
            )

            client.newCall(request(server)).execute().use { response ->
                assertEquals(401, response.code)
            }

            assertEquals(listOf("rejected-jwt"), rejectedTokens)
            assertEquals(1, server.requestCount)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `401 without a session token does not request invalidation`() {
        var invalidationCalls = 0
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(401))
        server.start()

        try {
            val client = client(
                tokenProvider = FakeTokenProvider(null),
                onUnauthorized = { invalidationCalls += 1 }
            )

            client.newCall(request(server)).execute().close()

            assertEquals(0, invalidationCalls)
            assertEquals(1, server.requestCount)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `403 preserves the authenticated session`() {
        var invalidationCalls = 0
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(403))
        server.start()

        try {
            val client = client(
                tokenProvider = FakeTokenProvider("valid-jwt"),
                onUnauthorized = { invalidationCalls += 1 }
            )

            client.newCall(request(server)).execute().use { response ->
                assertEquals(403, response.code)
            }

            assertEquals(0, invalidationCalls)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `delayed 401 cannot clear a newer authenticated session`() = runTest {
        val oldRequestArrived = CountDownLatch(1)
        val releaseOldResponse = CountDownLatch(1)
        val server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                oldRequestArrived.countDown()
                releaseOldResponse.await(5, TimeUnit.SECONDS)
                return MockResponse().setResponseCode(401)
            }
        }
        server.start()

        val executor = Executors.newSingleThreadExecutor()
        try {
            val storage = FakeSessionStorage()
            val sessionManager = DefaultSessionManager(storage)
            val oldSession = sampleSession("old-session")
            val newSession = oldSession.copy(name = "New login, same JWT")
            sessionManager.establish(oldSession)

            val client = client(
                tokenProvider = sessionManager,
                onUnauthorized = sessionManager::invalidateIfCurrent
            )
            val responseCode = executor.submit<Int> {
                client.newCall(request(server)).execute().use { it.code }
            }

            assertTrue(oldRequestArrived.await(5, TimeUnit.SECONDS))
            sessionManager.establish(newSession)
            releaseOldResponse.countDown()

            assertEquals(401, responseCode.get(5, TimeUnit.SECONDS))
            assertEquals(newSession.accessToken, sessionManager.currentToken())
            assertEquals(SessionState.Authenticated(newSession), sessionManager.state.value)
            assertEquals(newSession, storage.currentSession())
        } finally {
            releaseOldResponse.countDown()
            executor.shutdownNow()
            server.shutdown()
        }
    }

    private fun executeRequest(
        token: String?,
        authorizationHeader: String? = null,
        assertions: (RecordedRequest) -> Unit
    ) {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(200))
        server.start()

        try {
            val request = request(server, authorizationHeader)
            client(FakeTokenProvider(token)).newCall(request).execute().close()
            assertions(server.takeRequest())
        } finally {
            server.shutdown()
        }
    }

    private fun client(
        tokenProvider: TokenProvider,
        onUnauthorized: ((TokenSnapshot) -> Unit)? = null
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(BearerTokenInterceptor(tokenProvider, onUnauthorized))
            .build()
    }

    private fun request(
        server: MockWebServer,
        authorizationHeader: String? = null
    ): Request {
        return Request.Builder()
            .url(server.url("/api/v1/categorias"))
            .apply {
                authorizationHeader?.let { header("Authorization", it) }
            }
            .build()
    }

    private class FakeTokenProvider(
        private val token: String?
    ) : TokenProvider {

        override fun currentToken(): String? = token
    }

    private class FakeSessionStorage : SessionStorage {

        @Volatile
        private var session: AuthenticatedSession? = null

        override fun currentToken(): String? = session?.accessToken

        override fun currentSession(): AuthenticatedSession? = session

        override fun save(session: AuthenticatedSession) {
            this.session = session
        }

        override fun clear() {
            session = null
        }
    }

    private companion object {
        fun sampleSession(subject: String): AuthenticatedSession {
            val payload = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString("""{"sub":"$subject","exp":4102444800}""".toByteArray())
            return AuthenticatedSession(
                accessToken = "eyJhbGciOiJIUzI1NiJ9.$payload.signature-$subject",
                tokenType = "Bearer",
                userId = subject,
                email = "$subject@ojociudadano.cl",
                name = subject,
                roles = listOf("CIUDADANO")
            )
        }
    }
}
