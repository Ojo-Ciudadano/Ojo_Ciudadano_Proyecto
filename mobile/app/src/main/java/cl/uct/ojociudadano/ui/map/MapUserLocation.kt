package cl.uct.ojociudadano.ui.map

import cl.uct.ojociudadano.domain.model.GeoCoordinate

/**
 * Ubicación actual del usuario dentro de la pantalla de mapa.
 *
 * [focusRequestId] cambia cada vez que el usuario solicita centrar el mapa en
 * su posición. La pantalla lo usa como señal para animar la cámara incluso si
 * dos lecturas consecutivas entregan la misma coordenada.
 */
data class MapUserLocation(
    val coordinate: GeoCoordinate,
    val focusRequestId: Long = 0L
)
