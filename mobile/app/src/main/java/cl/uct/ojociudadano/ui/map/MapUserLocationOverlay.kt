package cl.uct.ojociudadano.ui.map

import cl.uct.ojociudadano.domain.model.GeoCoordinate
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/** Nivel de acercamiento usado al enfocar la ubicación del usuario. */
internal const val USER_LOCATION_FOCUS_ZOOM = 17.0

internal const val USER_LOCATION_FOCUS_DURATION_MILLIS = 800L

/**
 * Clave que distingue el marcador de ubicación del usuario de los marcadores
 * de incidentes. No es un [String] para que [syncIncidentMarkers] lo ignore y
 * no lo elimine al sincronizar el listado de incidentes.
 */
private val USER_LOCATION_MARKER_KEY = Any()

/**
 * Dibuja o actualiza la posición actual del usuario sobre [mapView].
 *
 * Solo existe un marcador de usuario: las lecturas posteriores reutilizan el
 * mismo overlay para no acumular pines. Pasar `null` retira la representación.
 */
internal fun syncUserLocationMarker(
    mapView: MapView,
    userLocation: MapUserLocation?
) {
    val current = mapView.overlays
        .filterIsInstance<Marker>()
        .firstOrNull { it.relatedObject === USER_LOCATION_MARKER_KEY }

    if (userLocation == null || !userLocation.coordinate.isValid) {
        if (current != null) {
            mapView.overlays.remove(current)
            mapView.invalidate()
        }
        return
    }

    val position = GeoPoint(
        userLocation.coordinate.latitude,
        userLocation.coordinate.longitude
    )

    if (current == null) {
        mapView.overlays.add(
            Marker(mapView).apply {
                relatedObject = USER_LOCATION_MARKER_KEY
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                title = "Tu ubicación actual"
                this.position = position
            }
        )
    } else if (current.position != position) {
        current.position = position
    }

    mapView.invalidate()
}

/**
 * Anima la cámara hacia [coordinate] con un nivel de zoom cercano.
 *
 * Se solicita en el hilo principal del mapa y no inicia ningún rastreo
 * continuo: es un único desplazamiento disparado por la acción del usuario.
 */
internal fun animateMapToUserLocation(
    mapView: MapView,
    coordinate: GeoCoordinate
) {
    if (!coordinate.isValid) return

    mapView.controller.animateTo(
        GeoPoint(coordinate.latitude, coordinate.longitude),
        USER_LOCATION_FOCUS_ZOOM,
        USER_LOCATION_FOCUS_DURATION_MILLIS
    )
}
