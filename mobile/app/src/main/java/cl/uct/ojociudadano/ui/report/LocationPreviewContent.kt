package cl.uct.ojociudadano.ui.report

import cl.uct.ojociudadano.domain.model.GeoCoordinate
import cl.uct.ojociudadano.domain.model.LocationResult
import java.util.Locale

/**
 * Datos listos para presentar la tarjeta de previsualización de ubicación.
 *
 * La tarjeta solo existe cuando el formulario conserva una coordenada válida.
 * El texto se calcula fuera de la composición para poder probar el formato, los
 * valores por defecto y el origen sin depender de la interfaz.
 */
internal data class LocationPreviewContent(
    val coordinate: GeoCoordinate,
    val coordinatesLabel: String,
    val addressLabel: String,
    val source: LocationSelectionSource,
    val sourceLabel: String
)

/**
 * Construye la información de la tarjeta de ubicación confirmada.
 *
 * Devuelve `null` mientras no exista una lectura válida del GPS o una selección
 * manual confirmada. La comuna, la dirección y la región se muestran solo si
 * tienen contenido, sin inventar datos cuando el autocompletado no respondió.
 */
internal fun CreateReportFormState.locationPreviewContent(): LocationPreviewContent? {
    val success = location as? LocationResult.Success ?: return null
    val coordinate = GeoCoordinate(success.latitude, success.longitude)
    if (!coordinate.isValid) return null

    return LocationPreviewContent(
        coordinate = coordinate,
        coordinatesLabel = locationCoordinatesLabel(coordinate),
        addressLabel = locationAddressLabel(referenceAddress, commune, region),
        source = locationSource,
        sourceLabel = locationSourceLabel(locationSource)
    )
}

internal fun locationCoordinatesLabel(coordinate: GeoCoordinate): String = buildString {
    append("Latitud: ")
    append(formatLocationCoordinate(coordinate.latitude))
    append("\nLongitud: ")
    append(formatLocationCoordinate(coordinate.longitude))
}

internal fun locationAddressLabel(
    referenceAddress: String,
    commune: String,
    region: String
): String {
    val parts = listOf(referenceAddress, commune, region)
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    return parts.joinToString(" · ").ifEmpty { PENDING_ADDRESS_LABEL }
}

internal fun locationSourceLabel(source: LocationSelectionSource): String = when (source) {
    LocationSelectionSource.DEVICE -> "Obtenida por GPS"
    LocationSelectionSource.MANUAL -> "Seleccionada en el mapa"
    LocationSelectionSource.NONE -> "Ubicación confirmada"
}

internal const val PENDING_ADDRESS_LABEL = "Dirección referencial pendiente"

private fun formatLocationCoordinate(value: Double): String =
    String.format(Locale.US, "%.6f", value)
