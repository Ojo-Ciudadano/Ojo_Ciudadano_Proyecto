package cl.uct.ojociudadano.data.remote.error

import java.io.IOException

sealed class ApiException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {

    class Network(
        cause: IOException
    ) : ApiException(
        message = "No fue posible conectar con el servidor",
        cause = cause
    )

    class Http(
        val statusCode: Int,
        val serverMessages: List<String>,
        val error: String?,
        val path: String?
    ) : ApiException(
        message = serverMessages
            .takeIf { it.isNotEmpty() }
            ?.joinToString("\n")
            ?: error
            ?: "Error HTTP $statusCode"
    )

    class Unexpected(
        cause: Throwable
    ) : ApiException(
        message = "Ocurrió un error inesperado al comunicarse con el servidor",
        cause = cause
    )
}
