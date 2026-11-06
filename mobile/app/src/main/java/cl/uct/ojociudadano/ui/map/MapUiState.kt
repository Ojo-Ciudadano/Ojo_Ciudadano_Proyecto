package cl.uct.ojociudadano.ui.map

/**
 * Estado de la representación de incidentes en el mapa.
 *
 * [Success] solo contiene incidentes con coordenadas válidas y sin duplicados.
 * [Empty] se usa cuando la consulta fue exitosa pero no hay ningún incidente
 * georreferenciable que representar.
 */
sealed interface MapUiState {

    data object Loading : MapUiState

    data class Success(
        val markers: List<IncidentMapMarker>,
        val selectedIncidentId: String? = null
    ) : MapUiState {

        /**
         * Marcador seleccionado actualmente, o `null` si no hay selección.
         *
         * Se deriva de [markers], de modo que el resumen nunca referencia un
         * incidente que ya no está representado en el mapa.
         */
        val selectedMarker: IncidentMapMarker?
            get() = selectedIncidentId?.let { selectedId ->
                markers.firstOrNull { it.incidentId == selectedId }
            }
    }

    data object Empty : MapUiState

    data class Error(
        val message: String
    ) : MapUiState
}
