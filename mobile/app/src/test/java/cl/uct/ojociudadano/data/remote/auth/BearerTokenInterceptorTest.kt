package cl.uct.ojociudadano.data.remote.auth

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
}
