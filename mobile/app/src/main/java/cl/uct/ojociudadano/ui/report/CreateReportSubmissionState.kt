package cl.uct.ojociudadano.ui.report

/**
 * Representa el avance del envío del formulario.
 *
 * La creación del incidente y la subida de sus evidencias son procesos
 * separados: el incidente se crea primero y solo entonces se intenta asociar
 * cada fotografía. Por eso el estado distingue entre ambas etapas y conserva
 * el identificador del incidente aunque alguna evidencia no pueda subirse.
 */
sealed interface CreateReportSubmissionState {
    data object Idle : CreateReportSubmissionState

    data object CreatingIncident : CreateReportSubmissionState

    data class UploadingEvidence(
        val incidentId: String,
        val processed: Int,
        val total: Int
    ) : CreateReportSubmissionState

    /**
     * El incidente fue creado y su evidencia quedó resuelta.
     *
     * [discardedEvidenceCount] es mayor que cero cuando el usuario decidió
     * finalizar con las fotografías que sí se subieron y descartar las
     * pendientes.
     */
    data class Success(
        val incidentId: String,
        val uploadedEvidenceCount: Int,
        val discardedEvidenceCount: Int = 0
    ) : CreateReportSubmissionState

    /**
     * El incidente ya fue creado, pero quedaron evidencias pendientes.
     *
     * [uploadedEvidenceCount] acumula las evidencias subidas en intentos
     * anteriores y [failedEvidenceCount] indica cuántas siguen sin subirse.
     */
    data class EvidenceUploadFailed(
        val incidentId: String,
        val uploadedEvidenceCount: Int,
        val failedEvidenceCount: Int,
        val message: String
    ) : CreateReportSubmissionState

    data class Error(val message: String) : CreateReportSubmissionState
}

/**
 * Indica si el formulario está ejecutando una operación remota y, por tanto,
 * debe bloquear el envío y las acciones sobre las fotografías.
 */
val CreateReportSubmissionState.isInProgress: Boolean
    get() = this is CreateReportSubmissionState.CreatingIncident ||
        this is CreateReportSubmissionState.UploadingEvidence

/**
 * Indica si el usuario todavía puede modificar el formulario.
 *
 * Después de crear el incidente el formulario queda bloqueado para evitar que
 * una edición habilite un segundo envío y genere un incidente duplicado.
 */
val CreateReportSubmissionState.canEditForm: Boolean
    get() = this is CreateReportSubmissionState.Idle ||
        this is CreateReportSubmissionState.Error
