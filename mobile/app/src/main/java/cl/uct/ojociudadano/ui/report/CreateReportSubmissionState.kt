package cl.uct.ojociudadano.ui.report

/**
 * Representa el resultado del envío remoto del formulario.
 *
 * Esta tarea solo modela el estado. La transición a Loading, Success o Error
 * se conectará cuando se implemente la creación remota del reporte.
 */
sealed interface CreateReportSubmissionState {
    data object Idle : CreateReportSubmissionState
    data object Loading : CreateReportSubmissionState
    data object Success : CreateReportSubmissionState
    data class Error(val message: String) : CreateReportSubmissionState
}
