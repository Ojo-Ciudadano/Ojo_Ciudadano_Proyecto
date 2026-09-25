package cl.uct.ojociudadano.data.remote.error

import java.io.IOException
import java.util.concurrent.CancellationException
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class ApiCallTest {

    @Test
    fun `successful call returns result`() = runTest {
        val result = executeApiCall {
            "respuesta"
        }

        assertEquals("respuesta", result)
    }

    @Test
    fun `io exception becomes network exception`() = runTest {
        val cause = IOException("Connection refused")

        val exception = expectException<ApiException.Network> {
            executeApiCall<String> {
                throw cause
            }
        }

        assertEquals(
            "No fue posible conectar con el servidor",
            exception.message
        )
        assertSame(cause, exception.cause)
    }

    @Test
    fun `http 400 preserves backend message array`() = runTest {
        val json = """
            {
              "statusCode": 400,
              "timestamp": "2026-09-25T10:00:00.000Z",
              "path": "/api/v1/incidentes",
              "error": "Bad Request",
              "message": [
                "El titulo es obligatorio",
                "La latitud debe ser un valor numerico"
              ]
            }
        """.trimIndent()

        val exception = expectException<ApiException.Http> {
            executeApiCall<String> {
                throw httpException(400, json)
            }
        }

        assertEquals(400, exception.statusCode)
        assertEquals(
            listOf(
                "El titulo es obligatorio",
                "La latitud debe ser un valor numerico"
            ),
            exception.serverMessages
        )
        assertEquals("Bad Request", exception.error)
        assertEquals("/api/v1/incidentes", exception.path)
        assertEquals(
            "El titulo es obligatorio\nLa latitud debe ser un valor numerico",
            exception.message
        )
    }

    @Test
    fun `http error preserves backend string message`() = runTest {
        val json = """
            {
              "statusCode": 404,
              "timestamp": "2026-09-25T10:00:00.000Z",
              "path": "/api/v1/incidentes/abc",
              "error": "Not Found",
              "message": "Incidente no encontrado"
            }
        """.trimIndent()

        val exception = expectException<ApiException.Http> {
            executeApiCall<String> {
                throw httpException(404, json)
            }
        }

        assertEquals(404, exception.statusCode)
        assertEquals(
            listOf("Incidente no encontrado"),
            exception.serverMessages
        )
        assertEquals("Not Found", exception.error)
        assertEquals(
            "/api/v1/incidentes/abc",
            exception.path
        )
        assertEquals("Incidente no encontrado", exception.message)
    }

    @Test
    fun `http error with invalid body uses status fallback`() = runTest {
        val exception = expectException<ApiException.Http> {
            executeApiCall<String> {
                throw httpException(503, "not-json")
            }
        }

        assertEquals(503, exception.statusCode)
        assertTrue(exception.serverMessages.isEmpty())
        assertEquals(null, exception.error)
        assertEquals(null, exception.path)
        assertEquals("Error HTTP 503", exception.message)
    }

    @Test
    fun `unexpected exception becomes unexpected api exception`() = runTest {
        val cause = IllegalStateException("Unexpected failure")

        val exception = expectException<ApiException.Unexpected> {
            executeApiCall<String> {
                throw cause
            }
        }

        assertEquals(
            "Ocurrió un error inesperado al comunicarse con el servidor",
            exception.message
        )
        assertSame(cause, exception.cause)
    }

    @Test
    fun `cancellation exception is not transformed`() = runTest {
        val cause = CancellationException("Cancelled")

        val exception = expectException<CancellationException> {
            executeApiCall<String> {
                throw cause
            }
        }

        assertSame(cause, exception)
    }

    private fun httpException(
        statusCode: Int,
        body: String
    ): HttpException {
        val responseBody = body.toResponseBody(
            "application/json".toMediaType()
        )

        return HttpException(
            Response.error<Any>(
                statusCode,
                responseBody
            )
        )
    }

    private suspend inline fun <reified T : Throwable> expectException(
        crossinline block: suspend () -> Unit
    ): T {
        try {
            block()
        } catch (throwable: Throwable) {
            if (throwable is T) {
                return throwable
            }

            throw AssertionError(
                "Se esperaba ${T::class.java.simpleName}, " +
                        "pero se obtuvo ${throwable::class.java.simpleName}",
                throwable
            )
        }

        throw AssertionError(
            "Se esperaba ${T::class.java.simpleName}, pero no se lanzó ninguna excepción"
        )
    }
}
