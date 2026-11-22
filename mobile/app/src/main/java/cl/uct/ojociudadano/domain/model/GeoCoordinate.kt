package cl.uct.ojociudadano.domain.model

/**
 * Coordenada geográfica expresada en grados decimales.
 *
 * No normaliza ni corrige los valores recibidos: [isValid] permite distinguir
 * una coordenada utilizable de una lectura inválida del mapa o del GPS.
 */
data class GeoCoordinate(
    val latitude: Double,
    val longitude: Double
) {

    val isValid: Boolean
        get() = isValidGeographicCoordinate(latitude, longitude)
}

/**
 * Verifica que ambos valores sean finitos y estén dentro de los rangos
 * geográficos admitidos: latitud [-90, 90] y longitud [-180, 180].
 */
fun isValidGeographicCoordinate(latitude: Double, longitude: Double): Boolean {
    if (!latitude.isFinite() || !longitude.isFinite()) return false
    return latitude in -90.0..90.0 && longitude in -180.0..180.0
}
