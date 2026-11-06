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
        val markers: List<IncidentMapMarker>
    ) : MapUiState

    data object Empty : MapUiState

    data class Error(
        val message: String
    ) : MapUiState
}
