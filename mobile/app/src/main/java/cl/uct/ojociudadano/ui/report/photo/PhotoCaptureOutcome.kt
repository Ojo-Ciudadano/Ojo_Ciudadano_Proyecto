package cl.uct.ojociudadano.ui.report.photo

import cl.uct.ojociudadano.data.media.EvidencePhotoFileStore
import cl.uct.ojociudadano.data.media.PendingPhotoCapture
import cl.uct.ojociudadano.data.model.IncidentPhotoEvidence

/**
 * Resultado de una captura iniciada con la cámara del sistema.
 */
sealed interface PhotoCaptureOutcome {

    data class Captured(val evidence: IncidentPhotoEvidence) : PhotoCaptureOutcome

    data object Cancelled : PhotoCaptureOutcome

    data class Failed(val message: String) : PhotoCaptureOutcome
}

/**
 * Traduce la respuesta de la actividad de cámara en un resultado de captura,
 * eliminando el archivo temporal cuando la fotografía no puede recuperarse.
 *
 * @param pending archivo reservado antes de abrir la cámara.
 * @param captured valor entregado por `ActivityResultContracts.TakePicture`.
 */
fun EvidencePhotoFileStore.resolveCaptureOutcome(
    pending: PendingPhotoCapture?,
    captured: Boolean
): PhotoCaptureOutcome {
    if (pending == null) {
        return PhotoCaptureOutcome.Cancelled
    }

    if (!captured) {
        delete(pending.fileName)
        return PhotoCaptureOutcome.Cancelled
    }

    val evidence = complete(pending)

    return if (evidence == null) {
        delete(pending.fileName)
        PhotoCaptureOutcome.Failed(PhotoCaptureErrors.RECOVER_PHOTO_FAILED)
    } else {
        PhotoCaptureOutcome.Captured(evidence)
    }
}
