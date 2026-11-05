package cl.uct.ojociudadano.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cl.uct.ojociudadano.data.model.Category
import cl.uct.ojociudadano.data.model.Incident
import cl.uct.ojociudadano.data.model.IncidentFilters
import cl.uct.ojociudadano.data.model.IncidentStatus
import cl.uct.ojociudadano.data.repository.CategoryRepository
import cl.uct.ojociudadano.data.repository.IncidentRepository
import cl.uct.ojociudadano.ui.reports.IncidentsUiState
import cl.uct.ojociudadano.ui.reports.incidentMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

data class AdminIncidentsState(
    val status: IncidentsUiState = IncidentsUiState.Loading,
    val incidents: List<Incident> = emptyList(),
    val filters: IncidentFilters = IncidentFilters(),
    val availableStates: List<IncidentStatus> = emptyList(),
    val availableCommunes: List<String> = emptyList(),
    val categories: List<Category> = emptyList(),
    val categoriesFailed: Boolean = false
)

class AdminIncidentsViewModel(
    private val incidentRepository: IncidentRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    private val _state = MutableStateFlow(AdminIncidentsState())
    val state = _state.asStateFlow()

    private var loadJob: Job? = null
    private var optionsJob: Job? = null
    private var categoriesJob: Job? = null

    // The selector catalogs are derived from one unfiltered response. Once that response
    // arrives we stop asking for it again while the ViewModel lives.
    private var optionsInitialized = false

    init {
        loadCategories()
        loadIncidents()
    }

    fun loadIncidents(filters: IncidentFilters = _state.value.filters) {
        // Deduplicate an in-flight identical request; a filter change cancels and supersedes it.
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            _state.update { it.copy(status = IncidentsUiState.Loading, incidents = emptyList(), filters = filters) }
            try {
                // General authenticated query; filters are only forwarded for the admin listing.
                val incidents = incidentRepository.getIncidents(filters)
                if (filters.isEmpty) optionsInitialized = true
                _state.update { current ->
                    current.copy(
                        status = if (incidents.isEmpty()) IncidentsUiState.Empty else IncidentsUiState.Success,
                        incidents = incidents,
                        // Options are derived once from the unfiltered data set so the
                        // selectors stay stable while a filter narrows the results.
                        availableStates = if (filters.isEmpty) deriveStates(incidents) else current.availableStates,
                        availableCommunes = if (filters.isEmpty) deriveCommunes(incidents) else current.availableCommunes
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _state.update { it.copy(status = IncidentsUiState.Error(exception.incidentMessage()), incidents = emptyList()) }
            }
        }
        // Selecting a filter before the first unfiltered response arrives cancels the only
        // query that populates the selectors, so the catalog is rebuilt by a separate job
        // that filter changes never cancel.
        if (!filters.isEmpty) ensureOptions()
    }

    fun applyFilters(filters: IncidentFilters) {
        if (filters == _state.value.filters) return
        // A late response must never overwrite the result of a newer filter selection.
        loadJob?.cancel()
        loadIncidents(filters)
    }

    fun clearFilters() = applyFilters(IncidentFilters())

    fun selectEstado(estadoId: String?) = applyFilters(_state.value.filters.copy(estadoId = estadoId))

    fun selectCategoria(categoriaId: String?) = applyFilters(_state.value.filters.copy(categoriaId = categoriaId))

    fun selectComuna(comuna: String?) = applyFilters(_state.value.filters.copy(comuna = comuna))

    /** Loads (or retries) the category catalog without touching the selected filters. */
    fun loadCategories() {
        if (categoriesJob?.isActive == true) return
        categoriesJob = viewModelScope.launch {
            try {
                val categories = categoryRepository.getCategories()
                _state.update { it.copy(categories = categories, categoriesFailed = false) }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                // A catalog failure must not block the incident listing, but it is exposed so
                // the user can retry it while keeping the filters already selected.
                _state.update { it.copy(categoriesFailed = true) }
            }
        }
    }

    private fun ensureOptions() {
        if (optionsInitialized) return
        if (optionsJob?.isActive == true) return
        optionsJob = viewModelScope.launch {
            try {
                val incidents = incidentRepository.getIncidents(IncidentFilters())
                optionsInitialized = true
                _state.update { current ->
                    current.copy(
                        availableStates = deriveStates(incidents),
                        availableCommunes = deriveCommunes(incidents)
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                // The listing remains usable; a later filter change retries the catalog.
            }
        }
    }

    private fun deriveStates(incidents: List<Incident>): List<IncidentStatus> =
        incidents.map { it.status }.distinctBy { it.id }.sortedBy { it.order }

    private fun deriveCommunes(incidents: List<Incident>): List<String> =
        incidents.map { it.location.commune }.filter { it.isNotBlank() }.distinct().sorted()
}

class AdminIncidentsViewModelFactory(
    private val incidentRepository: IncidentRepository,
    private val categoryRepository: CategoryRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminIncidentsViewModel::class.java)) {
            return AdminIncidentsViewModel(incidentRepository, categoryRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
