package cl.uct.ojociudadano.ui.report.photo

/**
 * Reglas del permiso de cámara desacopladas de Android y de la interfaz.
 *
 * Se aíslan aquí para poder probarlas sin dispositivo y para que el mensaje
 * mostrado al usuario no dependa de detalles del launcher de Compose.
 */
object CameraPermissionPolicy {

    /**
     * El permiso debe solicitarse mientras el sistema no lo haya concedido.
     */
    fun needsRequest(isGranted: Boolean): Boolean = !isGranted

    /**
     * Determina el mensaje de error tras una denegación.
     *
     * Cuando el sistema ya no permite volver a mostrar la solicitud
     * (`shouldShowRationale == false`), el permiso quedó bloqueado y solo puede
     * habilitarse desde los Ajustes de la aplicación.
     */
    fun denialMessage(shouldShowRationale: Boolean): String =
        if (shouldShowRationale) {
            PhotoCaptureErrors.PERMISSION_REQUIRED
        } else {
            PhotoCaptureErrors.PERMISSION_PERMANENTLY_DENIED
        }
}
