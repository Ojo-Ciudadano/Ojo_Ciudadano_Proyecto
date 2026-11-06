package cl.uct.ojociudadano.ui.map

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.uct.ojociudadano.data.repository.IncidentRepository
import cl.uct.ojociudadano.ui.reports.incidentMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Obtiene los incidentes desde la capa de datos y los transforma en marcadores.
 *
 * La pantalla solo consume [uiState]; no conoce Retrofit ni el API Gateway.
 * Los filtros de categoría y estado se aplican en memoria sobre los marcadores
 * cargados, por lo que cambiar una selección no realiza consultas adicionales.
 */
class MapViewModel(
    private val incidentRepository: IncidentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MapUiState>(MapUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var filters = MapIncidentFilters()

    init {
        loadIncidents()
    }

    fun loadIncidents() {
        val previous = _uiState.value as? MapUiState.Success
        val previousSelectedId = previous?.selectedIncidentId
        val previousFiltersVisible = previous?.filtersVisible ?: false
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = MapUiState.Loading

            try {
                val markers = incidentRepository.getIncidents().toMapMarkers()
                if (markers.isEmpty()) {
                    _uiState.value = MapUiState.Empty
                    return@launch
                }

                filters = filters.validatedAgainst(
                    categories = markers.availableCategoryOptions(),
                    statuses = markers.availableStatusOptions()
                )
                val success = MapUiState.Success(
                    markers = markers,
                    selectedIncidentId = previousSelectedId,
                    filters = filters,
                    filtersVisible = previousFiltersVisible
                )
                _uiState.value = success.copy(
                    selectedIncidentId = previousSelectedId?.takeIf { selectedId ->
                        success.visibleMarkers.any { it.incidentId == selectedId }
                    }
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                runCatching {
                    Log.e(
                        "MapViewModel",
                        "Error al cargar incidentes del mapa: " +
                            "type=${exception::class.java.name}, " +
                            "message=${exception.message}, " +
                            "cause=${exception.cause}",
                        exception
                    )
                }
                _uiState.value = MapUiState.Error(exception.incidentMessage())
            }
        }
    }

    /** Filtra por categoría; [categoryId] nulo vuelve a mostrar todas las categorías. */
    fun selectCategory(categoryId: String?) {
        updateFilters { current -> current.copy(categoryId = categoryId) }
    }

    /** Filtra por estado; [statusId] nulo vuelve a mostrar todos los estados. */
    fun selectStatus(statusId: String?) {
        updateFilters { current -> current.copy(statusId = statusId) }
    }

    /** Restablece todos los filtros y vuelve a representar los incidentes cargados. */
    fun clearFilters() {
        updateFilters { MapIncidentFilters() }
    }

    /**
     * Muestra u oculta los controles de filtrado sin modificar los criterios
     * seleccionados ni realizar nuevas consultas.
     */
    fun setFiltersVisible(visible: Boolean) {
        val current = _uiState.value as? MapUiState.Success ?: return
        if (current.filtersVisible == visible) return

        _uiState.value = current.copy(filtersVisible = visible)
    }

    /**
     * Marca como seleccionado el incidente representado por [incidentId].
     *
     * La información resumida ya vive en [MapUiState.Success.markers], por lo
     * que seleccionar un marcador no dispara consultas adicionales. Solo se
     * pueden seleccionar marcadores visibles con los filtros vigentes.
     */
    fun selectIncident(incidentId: String) {
        val current = _uiState.value as? MapUiState.Success ?: return
        if (current.visibleMarkers.none { it.incidentId == incidentId }) return

        _uiState.value = current.copy(selectedIncidentId = incidentId)
    }

    /** Limpia la selección actual sin modificar los marcadores cargados. */
    fun clearSelection() {
        val current = _uiState.value as? MapUiState.Success ?: return
        if (current.selectedIncidentId == null) return

        _uiState.value = current.copy(selectedIncidentId = null)
    }

    private inline fun updateFilters(
        transform: (MapIncidentFilters) -> MapIncidentFilters
    ) {
        val current = _uiState.value as? MapUiState.Success ?: return
        val next = transform(filters).validatedAgainst(
            categories = current.categoryOptions,
            statuses = current.statusOptions
        )
        if (next == filters) return

        filters = next
        val updated = current.copy(filters = next)
        _uiState.value = updated.copy(
            selectedIncidentId = updated.selectedIncidentId?.takeIf { selectedId ->
                updated.visibleMarkers.any { it.incidentId == selectedId }
            }
        )
    }
}
