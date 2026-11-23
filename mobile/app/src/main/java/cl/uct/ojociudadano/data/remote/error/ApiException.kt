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

    open class Http(
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

    /** Conserva los metadatos HTTP para los consumidores existentes. */
    class InsufficientPermissions(
        serverMessages: List<String> = emptyList(),
        error: String? = null,
        path: String? = null
    ) : Http(403, serverMessages, error, path) {
        override val message: String = "No tienes permisos suficientes para realizar esta operación"
    }

    class Unexpected(
        cause: Throwable
    ) : ApiException(
        message = "Ocurrió un error inesperado al comunicarse con el servidor",
        cause = cause
    )
}
