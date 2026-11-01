package cl.uct.ojociudadano.data.remote

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test

class RetrofitClientTest {

    @Test
    fun `create uses remote UCT base url by default`() {
        val retrofit = RetrofitClient.create()

        assertEquals(
            "https://api-ojociudadano.dev.censei.cl/",
            retrofit.baseUrl().toString()
        )
    }

    @Test
    fun `create normalizes api v1 suffix to avoid duplicated prefix`() {
        val retrofit = RetrofitClient.create(
            "https://api-ojociudadano.dev.censei.cl/api/v1/"
        )

        assertEquals(
            "https://api-ojociudadano.dev.censei.cl/",
            retrofit.baseUrl().toString()
        )
    }

    @Test
    fun `apiService sends exact request path when baseUrl ends with api v1`() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        server.start()

        try {
            val baseWithApiV1 = server.url("/api/v1/").toString()
            val apiService = RetrofitClient.createApiService(baseUrl = baseWithApiV1)

            apiService.getCategories()

            val recordedRequest = server.takeRequest()
            assertEquals("/api/v1/categorias", recordedRequest.path)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `create configures expected base url`() {
        val retrofit = RetrofitClient.create(
            "http://localhost:3000/"
        )

        assertEquals(
            "http://localhost:3000/",
            retrofit.baseUrl().toString()
        )
    }

    @Test
    fun `create rejects blank base url`() {
        assertThrows(IllegalArgumentException::class.java) {
            RetrofitClient.create("")
        }
    }

    @Test
    fun `create rejects unsupported protocol`() {
        assertThrows(IllegalArgumentException::class.java) {
            RetrofitClient.create(
                "ftp://localhost:3000/"
            )
        }
    }

    @Test
    fun `create rejects base url without trailing slash`() {
        assertThrows(IllegalArgumentException::class.java) {
            RetrofitClient.create(
                "http://localhost:3000"
            )
        }
    }

    @Test
    fun `createApiService creates ApiService`() {
        val apiService = RetrofitClient.createApiService()

        assertNotNull(apiService)
    }
}
