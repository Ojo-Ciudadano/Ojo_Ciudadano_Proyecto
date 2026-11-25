package cl.uct.ojociudadano.domain.model

/**
 * Datos descriptivos que pueden completarse a partir de una coordenada.
 *
 * Cada campo es opcional porque los proveedores de geocodificación no siempre
 * resuelven todos los niveles administrativos. Un campo ausente no debe
 * reemplazar el valor que el usuario haya escrito manualmente.
 */
data class ReverseGeocodedAddress(
    val referenceAddress: String? = null,
    val commune: String? = null,
    val region: String? = null
) {
    val hasAnyValue: Boolean
        get() = listOf(referenceAddress, commune, region)
            .any { !it.isNullOrBlank() }
}

/**
 * Resultado de una consulta de geocodificación inversa.
 */
sealed interface ReverseGeocodingResult {
    data class Success(val address: ReverseGeocodedAddress) : ReverseGeocodingResult
    data object NoResults : ReverseGeocodingResult
    data class Error(val message: String) : ReverseGeocodingResult
}
