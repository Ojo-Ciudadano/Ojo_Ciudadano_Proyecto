package cl.uct.ojociudadano.ui.map.picker

import cl.uct.ojociudadano.domain.model.GeoCoordinate
import cl.uct.ojociudadano.domain.model.LocationResult

/**
 * Valores iniciales del selector manual de ubicación.
 *
 * La cámara parte de la última ubicación válida del formulario. Si no existe
 * (GPS no disponible, permiso denegado o una lectura inválida), se usa el
 * centro de referencia de La Araucanía para que el selector siempre abra en
 * una posición útil.
 */
object ManualLocationPickerDefaults {

    val ARAUCANIA_CENTER = GeoCoordinate(
        latitude = -38.7359,
        longitude = -72.5904
    )

    const val INITIAL_ZOOM = 16.0

    fun initialCoordinate(location: LocationResult): GeoCoordinate {
        val available = location as? LocationResult.Success ?: return ARAUCANIA_CENTER
        val coordinate = GeoCoordinate(available.latitude, available.longitude)
        return coordinate.takeIf { it.isValid } ?: ARAUCANIA_CENTER
    }
}
