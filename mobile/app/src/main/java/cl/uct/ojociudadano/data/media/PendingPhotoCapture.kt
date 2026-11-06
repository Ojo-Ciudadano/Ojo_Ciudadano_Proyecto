package cl.uct.ojociudadano.data.media

import cl.uct.ojociudadano.data.model.IncidentPhotoEvidence

/**
 * Archivo temporal reservado para una captura de cámara en curso.
 *
 * El contenido solo existe después de que la aplicación de cámara escribe en
 * [localUri]; por eso la evidencia definitiva se construye con [toEvidence]
 * una vez recuperado el resultado de la captura.
 */
data class PendingPhotoCapture(
    val localUri: String,
    val fileName: String,
    val mimeType: String
) {

    fun toEvidence(sizeBytes: Long, capturedAt: Long): IncidentPhotoEvidence =
        IncidentPhotoEvidence(
            localUri = localUri,
            fileName = fileName,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
            capturedAt = capturedAt
        )
}
