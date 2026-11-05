package cl.uct.ojociudadano.ui.map

import cl.uct.ojociudadano.data.model.Incident

/**
 * Datos mínimos que el mapa necesita para representar un incidente.
 *
 * [incidentId] conserva la referencia al incidente de origen para futuras
 * interacciones (resumen, navegación al detalle o filtros).
 */
data class IncidentMapMarker(
    val incidentId: String,
    val latitude: Double,
    val longitude: Double
)

/** Verifica que las coordenadas sean finitas y estén dentro del rango geográfico válido. */
fun isValidMapCoordinate(latitude: Double, longitude: Double): Boolean {
    if (!latitude.isFinite() || !longitude.isFinite()) return false
    return latitude in -90.0..90.0 && longitude in -180.0..180.0
}

/**
 * Convierte un incidente en un marcador.
 *
 * Devuelve `null` cuando el incidente no tiene identificador o sus coordenadas
 * son inválidas, de modo que nunca se dibuja un marcador en una posición
 * incorrecta.
 */
fun Incident.toMapMarkerOrNull(): IncidentMapMarker? {
    if (id.isBlank()) return null

    val latitude = location.latitude
    val longitude = location.longitude
    if (!isValidMapCoordinate(latitude, longitude)) return null

    return IncidentMapMarker(
        incidentId = id,
        latitude = latitude,
        longitude = longitude
    )
}

/**
 * Transforma un listado de incidentes en marcadores válidos.
 *
 * Descarta incidentes sin coordenadas válidas y elimina duplicados por
 * identificador, conservando el orden entregado por la capa de datos.
 */
fun List<Incident>.toMapMarkers(): List<IncidentMapMarker> {
    val seenIds = HashSet<String>()
    val markers = ArrayList<IncidentMapMarker>(size)

    for (incident in this) {
        val marker = incident.toMapMarkerOrNull() ?: continue
        if (seenIds.add(marker.incidentId)) {
            markers.add(marker)
        }
    }

    return markers
}
