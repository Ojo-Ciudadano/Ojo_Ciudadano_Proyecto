package cl.uct.ojociudadano.ui.report

import cl.uct.ojociudadano.data.model.EvidenceCompatibilityIssue
import cl.uct.ojociudadano.data.model.EvidenceUploadConstraints
import cl.uct.ojociudadano.data.model.IncidentPhotoEvidence

/**
 * Mensajes que el formulario muestra cuando una o más evidencias no pueden
 * subirse después de crear el incidente.
 *
 * Se centralizan para que el ViewModel y la interfaz no dependan de textos
 * dispersos y para poder reutilizarlos en las pruebas.
 */
object EvidenceUploadErrors {

    /**
     * Mensaje específico para una fotografía que no cumple los límites del
     * backend. Se usa antes de crear el incidente para que el usuario pueda
     * corregir o reemplazar la selección.
     */
    fun compatibility(
        position: Int,
        photo: IncidentPhotoEvidence,
        issues: List<EvidenceCompatibilityIssue>
    ): String {
        val reasons = issues.joinToString("; ") { issue ->
            when (issue) {
                EvidenceCompatibilityIssue.FILE_UNAVAILABLE ->
                    "el archivo ya no está disponible"

                EvidenceCompatibilityIssue.UNSUPPORTED_FORMAT ->
                    "formato no compatible (usa JPEG, PNG o WebP)"

                EvidenceCompatibilityIssue.EMPTY_FILE ->
                    "el archivo está vacío"

                EvidenceCompatibilityIssue.FILE_TOO_LARGE ->
                    "supera el límite de ${EvidenceUploadConstraints.MAX_FILE_SIZE_MB} MB"
            }
        }

        return "Fotografía ${position.coerceAtLeast(1)} (${photo.fileName}): $reasons."
    }

    /**
     * Mensaje para un fallo parcial: el incidente existe y las fotografías
     * fallidas permanecen disponibles para reintentar su subida.
     *
     * [hasUncertainEvidence] agrega una advertencia cuando alguna petición pudo
     * haber llegado al servidor aunque la aplicación no recibiera confirmación.
     */
    fun partial(
        failed: Int,
        total: Int,
        hasUncertainEvidence: Boolean = false
    ): String = buildString {
        append(
            "El reporte fue creado, pero no se pudieron subir $failed de $total " +
                "fotografías. Puedes reintentar el envío de las evidencias pendientes."
        )

        if (hasUncertainEvidence) {
            append(" ")
            append(UNCERTAIN_RESULT)
        }
    }

    const val FILE_UNAVAILABLE =
        "Una de las fotografías ya no está disponible en el dispositivo."

    /**
     * Advertencia persistente dentro del estado de fallo parcial.
     */
    const val UNCERTAIN_RESULT =
        "Algunas evidencias no confirmaron si llegaron al servidor; " +
            "reintentarlas podría duplicarlas."

    /**
     * Texto del diálogo que el usuario debe aceptar antes de reenviar una
     * evidencia cuyo resultado es incierto.
     */
    const val UNCERTAIN_RETRY_WARNING =
        "Algunas fotografías no confirmaron si llegaron al servidor. " +
            "Reintentarlas podría crear evidencias duplicadas. ¿Deseas continuar?"
}
