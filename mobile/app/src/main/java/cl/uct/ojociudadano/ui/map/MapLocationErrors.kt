package cl.uct.ojociudadano.ui.map

import cl.uct.ojociudadano.domain.model.LocationResult

/**
 * Mensajes que explican por qué el mapa no puede centrarse en el usuario.
 *
 * Se mantienen separados de los mensajes del formulario de reportes porque
 * aquí la ubicación solo controla la cámara y no impide usar el mapa.
 */
object MapLocationErrors {

    const val MISSING_PERMISSIONS =
        "Se requieren permisos de ubicación para centrar el mapa en tu posición."

    const val NOT_AVAILABLE =
        "La ubicación del dispositivo está desactivada. Actívala para centrar el mapa."

    const val INVALID_COORDINATES =
        "La ubicación recibida no es válida. Inténtalo nuevamente."

    const val UNKNOWN =
        "No fue posible obtener tu ubicación. Inténtalo nuevamente."

    fun messageFor(result: LocationResult): String? = when (result) {
        is LocationResult.Success -> null
        is LocationResult.Error -> result.message.ifBlank { UNKNOWN }
        LocationResult.MissingPermissions -> MISSING_PERMISSIONS
        LocationResult.NotAvailable -> NOT_AVAILABLE
    }
}
