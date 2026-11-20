package cl.uct.ojociudadano.data.remote

import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RetrofitClientTimeoutsTest {

    @Test
    fun `widens timeouts above the OkHttp defaults`() {
        val timeouts = RetrofitClient.defaultTimeouts

        assertEquals(15, timeouts.connectSeconds)
        assertEquals(60, timeouts.readSeconds)
        assertEquals(120, timeouts.writeSeconds)
        assertTrue("La escritura debe superar la lectura", timeouts.writeSeconds > timeouts.readSeconds)
    }

    @Test
    fun `applies only to evidence uploads`() {
        assertTrue(RetrofitClient.isEvidenceUpload(uploadRequest()))
        assertFalse(
            "Una consulta GET no debe heredar los tiempos de subida",
            RetrofitClient.isEvidenceUpload(request("GET", "https://api.test/api/v1/incidentes/1/evidencias"))
        )
        assertFalse(
            "Otras escrituras conservan los tiempos por defecto",
            RetrofitClient.isEvidenceUpload(request("POST", "https://api.test/api/v1/incidentes"))
        )
    }

    private fun uploadRequest() = request(
        "POST",
        "https://api.test/api/v1/incidentes/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa/evidencias"
    )

    private fun request(method: String, url: String): Request =
        Request.Builder()
            .url(url)
            .method(method, if (method == "GET") null else "".toRequestBody())
            .build()
}
