package cl.uct.ojociudadano.ui.permission

/**
 * Reglas del permiso de ubicación desacopladas de Android y de la interfaz.
 *
 * Se aíslan aquí para poder probarlas sin dispositivo y para que la decisión de
 * solicitar el permiso no dependa de detalles del launcher de Compose.
 */
object LocationPermissionPolicy {

    /**
     * El permiso se considera concedido si el sistema otorgó ubicación precisa
     * o aproximada. Android permite solicitar ambas y el usuario puede aceptar
     * solo la aproximada.
     */
    fun isGranted(fineGranted: Boolean, coarseGranted: Boolean): Boolean =
        fineGranted || coarseGranted

    fun needsRequest(fineGranted: Boolean, coarseGranted: Boolean): Boolean =
        !isGranted(fineGranted, coarseGranted)
}
