package cl.uct.ojociudadano.ui.reports

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import cl.uct.ojociudadano.data.model.Category
import cl.uct.ojociudadano.data.model.Incident
import cl.uct.ojociudadano.data.repository.CategoryRepository
import cl.uct.ojociudadano.data.repository.IncidentRepository

import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.cancellation.CancellationException

import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class IncidentScope { ALL, MINE }

class ReportsViewModel(
    private val incidentRepository: IncidentRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _scope = MutableStateFlow(IncidentScope.MINE)
    val scope = _scope.asStateFlow()
    private var loadJob: Job? = null
    private var selectedCategoryId: String? = null
    private var selectedDate = DateFilter.RECENT

    fun selectScope(scope: IncidentScope) {
        if (_scope.value == scope) return
        _scope.value = scope
        loadJob?.cancel()
        loadJob = null
        loadIncidents()
    }

    // TODOS LOS INCIDENTES
    private val _allIncidents = MutableStateFlow<List<Incident>>(emptyList())

    // CONTROL DE ESTADOS
    private val _uiState = MutableStateFlow<IncidentsUiState>(IncidentsUiState.Loading )

    val uiState = _uiState.asStateFlow()

    // INCIDENTES
    private val _incidents = MutableStateFlow<List<Incident>>(emptyList())

    val incidents = _incidents.asStateFlow()

    // CATEGORIAS
    private val _categories = MutableStateFlow<List<Category>>(emptyList())

    val categories = _categories.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    init {
        loadIncidents()
        loadCategories()
    }

    fun refresh() = queryIncidents(refresh = true)

    fun loadIncidents() = queryIncidents(refresh = false)

    private fun queryIncidents(refresh: Boolean) {
        if (loadJob?.isActive == true) return
        val requestedScope = _scope.value
        _isRefreshing.value = refresh
        loadJob = viewModelScope.launch {
            if (!refresh) {
                _allIncidents.value = emptyList()
                _incidents.value = emptyList()
                _uiState.value = IncidentsUiState.Loading
            }

            try {
                val resultI = when (requestedScope) {
                    IncidentScope.ALL -> incidentRepository.getIncidents()
                    IncidentScope.MINE -> incidentRepository.getCurrentUserIncidents()
                }

                coroutineContext.ensureActive()
                _allIncidents.value = resultI
                filterIncidents(selectedCategoryId, selectedDate)

                // Estado vacío o exitoso
                _uiState.value = if (resultI.isEmpty()) {
                    IncidentsUiState.Empty
                } else {
                    IncidentsUiState.Success
                }

            } catch (exception: CancellationException) {
                throw exception

            } catch (exception: Exception) {

                runCatching {
                    Log.e(
                        "ReportsViewModel",
                        "Error al cargar incidentes: " +
                            "type=${exception::class.java.name}, " +
                            "message=${exception.message}, " +
                            "cause=${exception.cause}",
                        exception
                    )
                }

                coroutineContext.ensureActive()
                if (!refresh) {
                    _allIncidents.value = emptyList()
                    _incidents.value = emptyList()
                }

                // Estado de error
                _uiState.value = IncidentsUiState.Error(
                    message = exception.incidentMessage()
                )
            } finally {
                if (coroutineContext[Job]?.isActive == true) {
                    _isRefreshing.value = false
                }
            }
        }
    }

    fun loadCategories() {
        viewModelScope.launch {
            try {
                val resultC = categoryRepository.getCategories()
                _categories.value = resultC

            } catch (exception: CancellationException) {
                throw exception

            } catch (exception: Exception) {
                runCatching {
                    Log.e("ReportsViewModel", "Error al cargar categorías", exception)
                }
            }
        }
    }

    // Residual
    fun filterByCategory(category: Category?) {
        if (category == null) {
            _incidents.value = _allIncidents.value
            return
        }

        _incidents.value = _allIncidents.value.filter { incident ->
            incident.category.id == category.id
        }
    }

    // Residual
    fun filterByDate(filter: DateFilter) {

        val now = Instant.now()

        _incidents.value = when (filter) {

            DateFilter.RECENT -> {
                _allIncidents.value.sortedByDescending { incident ->
                    (runCatching { Instant.parse(incident.createdAt) }.getOrNull() ?: Instant.EPOCH)
                }
            }

            DateFilter.TODAY -> {
                val today = now
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()

                _allIncidents.value.filter { incident ->
                    (runCatching { Instant.parse(incident.createdAt) }.getOrNull() ?: Instant.EPOCH)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate() == today
                }
            }

            DateFilter.WEEK -> {
                val limit = now.minus(7, ChronoUnit.DAYS)

                _allIncidents.value.filter { incident ->
                    (runCatching { Instant.parse(incident.createdAt) }.getOrNull() ?: Instant.EPOCH)
                        .isAfter(limit)
                }
            }

            DateFilter.MONTH -> {
                val limit = now.minus(30, ChronoUnit.DAYS)

                _allIncidents.value.filter { incident ->
                    (runCatching { Instant.parse(incident.createdAt) }.getOrNull() ?: Instant.EPOCH)
                        .isAfter(limit)
                }
            }

            DateFilter.YEAR -> {
                val limit = now.minus(365, ChronoUnit.DAYS)

                _allIncidents.value.filter { incident ->
                    (runCatching { Instant.parse(incident.createdAt) }.getOrNull() ?: Instant.EPOCH)
                        .isAfter(limit)
                }
            }
        }
    }

    fun filterIncidents(
        categoryId: String?,
        dateFilter: DateFilter
    ) {
        selectedCategoryId = categoryId
        selectedDate = dateFilter
        var filtered = _allIncidents.value

        // Filtrar por categoría
        if (categoryId != null) {
            filtered = filtered.filter { incident ->
                incident.category.id == categoryId
            }
        }

        // Filtrar por fecha
        val now = Instant.now()

        filtered = when (dateFilter) {

            DateFilter.RECENT -> {
                filtered.sortedByDescending { incident ->
                    (runCatching { Instant.parse(incident.createdAt) }.getOrNull() ?: Instant.EPOCH)
                }
            }

            DateFilter.TODAY -> {
                val today = now
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()

                filtered.filter { incident ->
                    (runCatching { Instant.parse(incident.createdAt) }.getOrNull() ?: Instant.EPOCH)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate() == today
                }
            }

            DateFilter.WEEK -> {
                val limit = now.minus(7, ChronoUnit.DAYS)

                filtered.filter { incident ->
                    (runCatching { Instant.parse(incident.createdAt) }.getOrNull() ?: Instant.EPOCH)
                        .isAfter(limit)
                }
            }

            DateFilter.MONTH -> {
                val limit = now.minus(30, ChronoUnit.DAYS)

                filtered.filter { incident ->
                    (runCatching { Instant.parse(incident.createdAt) }.getOrNull() ?: Instant.EPOCH)
                        .isAfter(limit)
                }
            }

            DateFilter.YEAR -> {
                val limit = now.minus(365, ChronoUnit.DAYS)

                filtered.filter { incident ->
                    (runCatching { Instant.parse(incident.createdAt) }.getOrNull() ?: Instant.EPOCH)
                        .isAfter(limit)
                }
            }
        }

        _incidents.value = filtered
    }
}
