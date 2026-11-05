package cl.uct.ojociudadano.ui.report

import cl.uct.ojociudadano.domain.model.LocationResult

/**
 * Mensajes que explican por qué el formulario no puede asociar una ubicación
 * GPS válida al incidente.
 */
object CreateReportLocationErrors {

    const val NOT_AVAILABLE =
        "La ubicación del dispositivo está desactivada. Actívala para continuar."

    const val MISSING_PERMISSIONS =
        "Se requieren permisos de ubicación para crear el reporte. " +
            "Si el sistema no vuelve a solicitarlos, habilítalos desde Ajustes."

    const val UNKNOWN =
        "No fue posible obtener tu ubicación. Inténtalo nuevamente."

    fun messageFor(result: LocationResult): String? = when (result) {
        is LocationResult.Success -> null
        is LocationResult.Error -> result.message.ifBlank { UNKNOWN }
        LocationResult.MissingPermissions -> MISSING_PERMISSIONS
        LocationResult.NotAvailable -> NOT_AVAILABLE
    }
}
