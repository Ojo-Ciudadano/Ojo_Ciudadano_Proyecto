package cl.uct.ojociudadano.data.remote.auth

import cl.uct.ojociudadano.data.session.AuthenticatedSession
import cl.uct.ojociudadano.data.session.SessionStorage
import cl.uct.ojociudadano.data.session.TokenProvider
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BearerTokenInterceptorTest {

    @Test
    fun `adds bearer token to request when a token is available`() {
        executeRequest(token = "jwt-value") { request ->
            assertEquals("Bearer jwt-value", request.getHeader("Authorization"))
        }
    }

    @Test
    fun `does not add authorization header when no token is available`() {
        executeRequest(token = null) { request ->
            assertNull(request.getHeader("Authorization"))
        }
    }

    @Test
    fun `clears session and performs single controlled retry on 401`() {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        server.start()

        try {
            val storage = FakeSessionStorage("stale-local-jwt")
            var refreshCalls = 0
            val client = OkHttpClient.Builder()
                .addInterceptor(
                    BearerTokenInterceptor(
                        tokenProvider = storage,
                        onUnauthorizedRefreshToken = {
                            refreshCalls += 1
                            storage.save(sampleSession("fresh-uct-jwt"))
                            "fresh-uct-jwt"
                        }
                    )
                )
                .build()

            val request = Request.Builder()
                .url(server.url("/api/v1/categorias"))
                .build()

            client.newCall(request).execute().use { response ->
                assertEquals(200, response.code)
            }

            val firstAttempt = server.takeRequest()
            val retryAttempt = server.takeRequest()

            assertEquals("Bearer stale-local-jwt", firstAttempt.getHeader("Authorization"))
            assertEquals("Bearer fresh-uct-jwt", retryAttempt.getHeader("Authorization"))
            assertEquals(1, refreshCalls)
            assertEquals("fresh-uct-jwt", storage.currentToken())
        } finally {
            server.shutdown()
        }
    }

    private fun executeRequest(
        token: String?,
        assertions: (okhttp3.mockwebserver.RecordedRequest) -> Unit
    ) {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(200))
        server.start()

        try {
            val client = OkHttpClient.Builder()
                .addInterceptor(BearerTokenInterceptor(FakeTokenProvider(token)))
                .build()
            val request = Request.Builder()
                .url(server.url("/api/v1/categorias"))
                .build()

            client.newCall(request).execute().close()

            assertions(server.takeRequest())
        } finally {
            server.shutdown()
        }
    }

    private class FakeTokenProvider(
        private val token: String?
    ) : TokenProvider {

        override fun currentToken(): String? = token
    }

    private class FakeSessionStorage(
        initialToken: String?
    ) : SessionStorage {

        private var session: AuthenticatedSession? = initialToken?.let(::sampleSession)

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
        fun sampleSession(token: String) = AuthenticatedSession(
            accessToken = token,
            tokenType = "Bearer",
            userId = "user-id",
            email = "dev@ojociudadano.cl",
            name = "Dev",
            roles = listOf("CIUDADANO")
        )
    }
}
