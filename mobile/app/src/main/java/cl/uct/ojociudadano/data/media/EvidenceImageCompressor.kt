package cl.uct.ojociudadano.data.media

import java.io.File

/**
 * Motivos por los que una fotografía no puede procesarse antes de subirse.
 *
 * Se modelan por separado de los mensajes de interfaz para que la capa de
 * datos y el ViewModel puedan decidir el destino de la evidencia sin depender
 * de textos.
 */
enum class EvidenceImageFailure {
    /** El archivo no existe, está vacío o dejó de ser legible. */
    UNAVAILABLE,

    /** El contenido no es una imagen decodificable. */
    CORRUPT,

    /** El formato no está entre los aceptados por el backend. */
    UNSUPPORTED_FORMAT,

    /** Aun comprimiendo no fue posible dejarla por debajo del límite. */
    CANNOT_FIT
}

/**
 * Error controlado del procesamiento de imágenes.
 *
 * Extiende [IllegalArgumentException] a propósito: es un rechazo local
 * determinista, no un resultado incierto de red, y el flujo de creación de
 * reportes ya clasifica esta familia como fallo confirmado.
 */
class EvidenceImageException(
    val failure: EvidenceImageFailure,
    message: String,
    cause: Throwable? = null
) : IllegalArgumentException(message, cause)

/**
 * Resultado de preparar una fotografía para su envío.
 *
 * [isTemporary] indica que la capa de datos debe borrar [file] cuando termine
 * la petición; si es `false`, [file] es el archivo original del formulario.
 */
data class CompressedEvidence(
    val file: File,
    val mimeType: String,
    val isTemporary: Boolean
)

/**
 * Prepara fotografías para la subida de evidencias.
 *
 * La implementación real redimensiona y comprime; existe para que la capa de
 * datos dependa de una abstracción inyectable y testeable.
 */
interface EvidenceImageCompressor {

    /**
     * Entrega un archivo subible con su MIME definitivo.
     *
     * @throws EvidenceImageException si la imagen no puede procesarse.
     */
    suspend fun compress(file: File, mimeType: String): CompressedEvidence
}

/**
 * Implementación neutra usada cuando no se configura un compresor.
 *
 * Mantiene el comportamiento anterior: envía el archivo tal cual. Existe para
 * no obligar a las pruebas de la capa de datos a construir un compresor real.
 */
object PassthroughEvidenceImageCompressor : EvidenceImageCompressor {
    override suspend fun compress(file: File, mimeType: String): CompressedEvidence =
        CompressedEvidence(file = file, mimeType = mimeType, isTemporary = false)
}
