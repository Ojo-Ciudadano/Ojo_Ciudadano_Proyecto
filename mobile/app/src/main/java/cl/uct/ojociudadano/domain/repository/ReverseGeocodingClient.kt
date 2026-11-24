package cl.uct.ojociudadano.domain.repository

import cl.uct.ojociudadano.domain.model.ReverseGeocodingResult

/**
 * Servicio desacoplado que traduce una coordenada en datos descriptivos.
 *
 * Las implementaciones deben ejecutar la consulta fuera del hilo principal y
 * nunca lanzar excepciones hacia el formulario: un fallo se informa mediante
 * [ReverseGeocodingResult.Error] para que el reporte siga siendo editable.
 */
interface ReverseGeocodingClient {
    suspend fun reverseGeocode(
        latitude: Double,
        longitude: Double
    ): ReverseGeocodingResult

    companion object {
        /**
         * Implementación neutra para escenarios donde no hay geocodificación
         * disponible. Permite conservar el formulario funcional sin red.
         */
        val Unavailable: ReverseGeocodingClient = object : ReverseGeocodingClient {
            override suspend fun reverseGeocode(
                latitude: Double,
                longitude: Double
            ): ReverseGeocodingResult = ReverseGeocodingResult.NoResults
        }
    }
}
