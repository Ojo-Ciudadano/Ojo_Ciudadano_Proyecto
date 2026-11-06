package cl.uct.ojociudadano.ui.reports.evidence

import cl.uct.ojociudadano.data.remote.error.ApiException
import cl.uct.ojociudadano.data.session.SessionRequiredException
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Test

class EvidencePhotoErrorsTest {

    @Test
    fun `session required maps to an expired session message`() {
        assertEquals(
            "Tu sesión expiró. Inicia sesión nuevamente.",
            evidencePhotoErrorMessage(SessionRequiredException())
        )
    }

    @Test
    fun `unauthorized and forbidden responses have citizen friendly messages`() {
        assertEquals(
            "Tu sesión expiró. Inicia sesión nuevamente.",
            evidencePhotoErrorMessage(http(401))
        )
        assertEquals(
            "No tienes autorización para ver esta fotografía.",
            evidencePhotoErrorMessage(http(403))
        )
    }

    @Test
    fun `missing evidence maps to a no longer available message`() {
        assertEquals(
            "La fotografía ya no está disponible.",
            evidencePhotoErrorMessage(http(404))
        )
    }

    @Test
    fun `storage not configured maps to a controlled unavailable message`() {
        assertEquals(
            "El almacenamiento de evidencias aún no está disponible.",
            evidencePhotoErrorMessage(http(503))
        )
    }

    @Test
    fun `server errors map to a temporary unavailable message`() {
        assertEquals(
            "El servicio de evidencias no está disponible temporalmente.",
            evidencePhotoErrorMessage(http(500))
        )
        assertEquals(
            "El servicio de evidencias no está disponible temporalmente.",
            evidencePhotoErrorMessage(http(502))
        )
    }

    @Test
    fun `other http errors keep the status code`() {
        assertEquals(
            "No se pudo cargar la fotografía (HTTP 400).",
            evidencePhotoErrorMessage(http(400))
        )
    }

    @Test
    fun `network and io errors map to a connection message`() {
        val expected =
            "No fue posible conectar con el servidor. Revisa tu conexión e intenta nuevamente."

        assertEquals(expected, evidencePhotoErrorMessage(ApiException.Network(IOException())))
        assertEquals(expected, evidencePhotoErrorMessage(IOException()))
    }

    @Test
    fun `unexpected errors map to a generic message`() {
        assertEquals(
            "No se pudo cargar la fotografía.",
            evidencePhotoErrorMessage(IllegalStateException("inesperado"))
        )
    }

    private fun http(statusCode: Int) = ApiException.Http(
        statusCode = statusCode,
        serverMessages = emptyList(),
        error = null,
        path = null
    )
}
