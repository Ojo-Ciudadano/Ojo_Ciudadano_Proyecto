package cl.uct.ojociudadano.ui.map

/**
 * Resuelve el identificador que puede abrirse en el detalle desde el resumen.
 *
 * La selección vigente del mapa es la fuente de verdad: se descarta cualquier
 * solicitud que no coincida con el marcador seleccionado actualmente, de modo
 * que una acción emitida por una composición anterior nunca navega a otro
 * incidente. También se descartan identificadores vacíos o en blanco.
 */
fun MapUiState.Success.selectedDetailIncidentId(requestedIncidentId: String?): String? {
    val requested = requestedIncidentId?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return selectedMarker
        ?.incidentId
        ?.trim()
        ?.takeIf { it.isNotEmpty() && it == requested }
}
