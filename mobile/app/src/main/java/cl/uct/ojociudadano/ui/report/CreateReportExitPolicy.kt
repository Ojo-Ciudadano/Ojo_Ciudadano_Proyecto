package cl.uct.ojociudadano.ui.report

import cl.uct.ojociudadano.data.model.IncidentPhotoEvidence

/**
 * Riesgo de perder evidencias al abandonar el formulario de creación.
 *
 * Se calcula a partir del estado de envío y de las fotografías visibles. La
 * navegación lo usa para pedir confirmación antes de destruir el ViewModel,
 * porque [CreateReportViewModel.onCleared] elimina los archivos temporales.
 */
data class CreateReportExitRisk(
    val pendingEvidenceCount: Int,
    val incidentAlreadyCreated: Boolean,
    val isUploading: Boolean
)

/**
 * Decide cuándo una salida del formulario debe confirmarse.
 *
 * Solo se protegen las salidas con evidencias que todavía podrían subirse o
 * reintentarse. Un formulario vacío, un envío exitoso o un error de creación
 * no bloquean la navegación.
 */
object CreateReportExitPolicy {

    fun riskOf(
        state: CreateReportSubmissionState,
        visiblePhotos: List<IncidentPhotoEvidence>
    ): CreateReportExitRisk? = when (state) {
        is CreateReportSubmissionState.EvidenceUploadFailed ->
            state.pendingEvidence
                .takeIf { it.isNotEmpty() }
                ?.let { pendingEvidence ->
                    CreateReportExitRisk(
                        pendingEvidenceCount = pendingEvidence.size,
                        incidentAlreadyCreated = true,
                        isUploading = false
                    )
                }

        is CreateReportSubmissionState.UploadingEvidence ->
            state.total
                .takeIf { it > 0 }
                ?.let { total ->
                    CreateReportExitRisk(
                        pendingEvidenceCount = total,
                        incidentAlreadyCreated = true,
                        isUploading = true
                    )
                }

        CreateReportSubmissionState.CreatingIncident ->
            visiblePhotos
                .takeIf { it.isNotEmpty() }
                ?.let { photos ->
                    CreateReportExitRisk(
                        pendingEvidenceCount = photos.size,
                        incidentAlreadyCreated = false,
                        isUploading = false
                    )
                }

        else -> null
    }

    fun message(risk: CreateReportExitRisk): String = when {
        risk.isUploading -> uploadingMessage(risk.pendingEvidenceCount)
        risk.incidentAlreadyCreated -> createdMessage(risk.pendingEvidenceCount)
        else -> creatingMessage(risk.pendingEvidenceCount)
    }

    private fun uploadingMessage(pendingEvidenceCount: Int): String = buildString {
        append("El reporte ya fue creado. ")

        if (pendingEvidenceCount == 1) {
            append(
                "Queda 1 fotografía pendiente de subir; si sales ahora, se " +
                    "cancelará la subida y se descartará la que no se haya enviado."
            )
        } else {
            append(
                "Quedan $pendingEvidenceCount fotografías pendientes de subir; " +
                    "si sales ahora, se cancelará la subida y se descartarán las " +
                    "que no se hayan enviado."
            )
        }
    }

    private fun createdMessage(pendingEvidenceCount: Int): String = buildString {
        append("El reporte ya fue creado, pero ")

        if (pendingEvidenceCount == 1) {
            append(
                "queda 1 fotografía sin subir. Si sales ahora, se descartará " +
                    "y no podrás reintentarla."
            )
        } else {
            append(
                "quedan $pendingEvidenceCount fotografías sin subir. Si sales " +
                    "ahora, se descartarán y no podrás reintentarlas."
            )
        }
    }

    private fun creatingMessage(pendingEvidenceCount: Int): String = buildString {
        append("El reporte se está creando")

        if (pendingEvidenceCount > 0) {
            append(" con ${photoCount(pendingEvidenceCount)}")
        }

        append(
            ". Si sales ahora, se perderán las fotografías seleccionadas y no " +
                "podrás reintentarlas."
        )
    }

    private fun photoCount(count: Int): String =
        if (count == 1) "1 fotografía" else "$count fotografías"
}
