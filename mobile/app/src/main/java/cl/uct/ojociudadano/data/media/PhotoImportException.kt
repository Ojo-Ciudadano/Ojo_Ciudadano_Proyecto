package cl.uct.ojociudadano.data.media

/**
 * Motivo por el que una fotografía existente no pudo importarse.
 *
 * Se separa del mensaje para que la interfaz pueda traducir cada caso sin
 * depender de excepciones de Android como [SecurityException] o
 * [java.io.IOException].
 */
enum class PhotoImportFailure {
    NOT_AN_IMAGE,
    EMPTY_FILE,
    FILE_TOO_LARGE,
    UNREADABLE
}

/**
 * Error controlado al copiar una fotografía desde el selector del dispositivo.
 */
class PhotoImportException(
    val failure: PhotoImportFailure,
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
