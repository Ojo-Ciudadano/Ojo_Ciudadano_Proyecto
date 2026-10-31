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
     */
    fun partial(failed: Int, total: Int): String =
        "El reporte fue creado, pero no se pudieron subir $failed de $total " +
            "fotografías. Puedes reintentar el envío de las evidencias pendientes."

    const val FILE_UNAVAILABLE =
        "Una de las fotografías ya no está disponible en el dispositivo."
}
