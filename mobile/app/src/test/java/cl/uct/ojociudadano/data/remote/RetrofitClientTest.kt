package cl.uct.ojociudadano.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.Assert.assertNotNull

class RetrofitClientTest {

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
        val apiService = RetrofitClient.createApiService(
            "http://localhost:3000/"
        )

        assertNotNull(apiService)
    }
}
