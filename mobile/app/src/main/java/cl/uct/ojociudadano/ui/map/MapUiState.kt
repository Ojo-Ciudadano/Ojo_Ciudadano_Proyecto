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
        val selectedIncidentId: String? = null,
        val filters: MapIncidentFilters = MapIncidentFilters()
    ) : MapUiState {

        /** Marcadores que cumplen los filtros y deben dibujarse sobre el mapa. */
        val visibleMarkers: List<IncidentMapMarker>
            get() = markers.filteredBy(filters)

        /** Opciones de categoría derivadas de los incidentes ya cargados. */
        val categoryOptions: List<MapFilterOption>
            get() = markers.availableCategoryOptions()

        /** Opciones de estado derivadas de los incidentes ya cargados. */
        val statusOptions: List<MapFilterOption>
            get() = markers.availableStatusOptions()

        /** Hay datos georreferenciados, pero ningún incidente cumple los filtros. */
        val isFilteredEmpty: Boolean
            get() = markers.isNotEmpty() && visibleMarkers.isEmpty()

        /**
         * Marcador seleccionado actualmente, o `null` si no hay selección.
         *
         * Se deriva de [visibleMarkers], de modo que el resumen nunca referencia
         * un incidente que ya no está representado en el mapa.
         */
        val selectedMarker: IncidentMapMarker?
            get() = selectedIncidentId?.let { selectedId ->
                visibleMarkers.firstOrNull { it.incidentId == selectedId }
            }
    }

    data object Empty : MapUiState

    data class Error(
        val message: String
    ) : MapUiState
}
