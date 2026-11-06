package cl.uct.ojociudadano.ui.map

import cl.uct.ojociudadano.data.model.Incident

/**
 * Información resumida del incidente asociado a un marcador.
 *
 * Todos los campos son opcionales porque la respuesta puede traer información
 * parcial. Los valores en blanco se normalizan a `null` para que la interfaz
 * omita los datos no disponibles.
 */
data class IncidentMapSummary(
    val title: String? = null,
    val description: String? = null,
    val categoryId: String? = null,
    val categoryName: String? = null,
    val statusId: String? = null,
    val statusName: String? = null,
    val createdAt: String? = null,
    val referenceAddress: String? = null
)

/**
 * Datos mínimos que el mapa necesita para representar un incidente.
 *
 * [incidentId] conserva la referencia al incidente de origen para el resumen y
 * para futuras interacciones (navegación al detalle o filtros). [summary]
 * reutiliza la información ya cargada para el mapa, evitando consultas extra al
 * seleccionar un marcador.
 */
data class IncidentMapMarker(
    val incidentId: String,
    val latitude: Double,
    val longitude: Double,
    val summary: IncidentMapSummary = IncidentMapSummary()
)

private fun String?.cleanedOrNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

private fun Incident.toMapSummary(): IncidentMapSummary = IncidentMapSummary(
    title = title.cleanedOrNull(),
    description = description.cleanedOrNull(),
    categoryId = category.id.cleanedOrNull(),
    categoryName = category.name.cleanedOrNull(),
    statusId = status.id.cleanedOrNull(),
    statusName = status.name.cleanedOrNull(),
    createdAt = createdAt.cleanedOrNull(),
    referenceAddress = location.referenceAddress.cleanedOrNull()
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
        longitude = longitude,
        summary = toMapSummary()
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
