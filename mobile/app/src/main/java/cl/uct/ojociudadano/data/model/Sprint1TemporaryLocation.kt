package cl.uct.ojociudadano.data.model

/**
 * Ubicación controlada de desarrollo mientras Sprint 1 no cuenta con GPS.
 *
 * Debe reemplazarse por la ubicación del dispositivo cuando se implemente el
 * flujo de permisos y localización real.
 */
object Sprint1TemporaryLocation {

    val incidentLocation = CreateIncidentLocationInput(
        latitude = -38.7359,
        longitude = -72.5904,
        referenceAddress = "Ubicación de prueba Sprint 1",
        commune = "Temuco",
        region = "La Araucanía",
        sector = "Centro"
    )
}
