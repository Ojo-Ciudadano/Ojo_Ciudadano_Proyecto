package cl.uct.ojociudadano.ui.report.photo

/**
 * Mensajes que el formulario muestra cuando la captura no puede completarse.
 *
 * Se centralizan aquí para que la capa de captura no dependa de textos
 * dispersos en la interfaz y para poder reutilizarlos en las pruebas.
 */
object PhotoCaptureErrors {

    const val PREPARE_FILE_FAILED = "No fue posible preparar el archivo de la fotografía"

    const val CAMERA_UNAVAILABLE = "No se encontró una aplicación de cámara en el dispositivo"

    const val OPEN_CAMERA_FAILED = "No fue posible abrir la cámara"

    const val RECOVER_PHOTO_FAILED = "No se pudo recuperar la fotografía capturada"
}
