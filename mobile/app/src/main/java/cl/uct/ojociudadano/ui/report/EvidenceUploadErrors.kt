package cl.uct.ojociudadano.ui.report

/**
 * Mensajes que el formulario muestra cuando una o más evidencias no pueden
 * subirse después de crear el incidente.
 *
 * Se centralizan para que el ViewModel y la interfaz no dependan de textos
 * dispersos y para poder reutilizarlos en las pruebas.
 */
object EvidenceUploadErrors {

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
