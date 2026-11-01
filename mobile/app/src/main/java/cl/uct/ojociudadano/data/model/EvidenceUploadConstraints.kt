package cl.uct.ojociudadano.data.model

import java.util.Locale

/**
 * Motivos por los que una fotografía local no puede subirse como evidencia.
 *
 * Se separan de los mensajes de interfaz para que la política de
 * compatibilidad pueda reutilizarse desde el formulario y desde la capa de
 * datos sin duplicar reglas.
 */
enum class EvidenceCompatibilityIssue {
    FILE_UNAVAILABLE,
    UNSUPPORTED_FORMAT,
    EMPTY_FILE,
    FILE_TOO_LARGE
}

/**
 * Límites que debe cumplir una evidencia para que el backend la acepte.
 *
 * El formulario usa esta política antes de crear el incidente y
 * [cl.uct.ojociudadano.data.repository.RemoteIncidentRepository] la reutiliza
 * como defensa en profundidad. Así los límites de la selección local y los del
 * backend no vuelven a divergir.
 */
object EvidenceUploadConstraints {

    const val MAX_FILE_SIZE_MB = 10L
    const val MAX_FILE_SIZE_BYTES = MAX_FILE_SIZE_MB * 1024L * 1024L

    val SUPPORTED_MIME_TYPES = setOf(
        "image/jpeg",
        "image/png",
        "image/webp"
    )

    fun isSupportedMimeType(mimeType: String): Boolean =
        mimeType.lowercase(Locale.ROOT) in SUPPORTED_MIME_TYPES

    /**
     * Entrega todos los problemas aplicables a una fotografía.
     *
     * [fileAvailable] lo informa el almacén temporal; el tamaño se toma de la
     * metadata local, que se genera a partir del archivo real al capturarlo o
     * importarlo.
     */
    fun issuesFor(
        photo: IncidentPhotoEvidence,
        fileAvailable: Boolean
    ): List<EvidenceCompatibilityIssue> = buildList {
        if (!fileAvailable) {
            add(EvidenceCompatibilityIssue.FILE_UNAVAILABLE)
        }
        if (!isSupportedMimeType(photo.mimeType)) {
            add(EvidenceCompatibilityIssue.UNSUPPORTED_FORMAT)
        }
        if (photo.sizeBytes <= 0L) {
            add(EvidenceCompatibilityIssue.EMPTY_FILE)
        }
        if (photo.sizeBytes > MAX_FILE_SIZE_BYTES) {
            add(EvidenceCompatibilityIssue.FILE_TOO_LARGE)
        }
    }
}
