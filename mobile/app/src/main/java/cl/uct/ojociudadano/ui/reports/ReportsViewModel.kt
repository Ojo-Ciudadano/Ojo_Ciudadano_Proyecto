package cl.uct.ojociudadano.ui.reports

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import cl.uct.ojociudadano.data.model.Category
import cl.uct.ojociudadano.data.model.Incident
import cl.uct.ojociudadano.data.repository.CategoryRepository
import cl.uct.ojociudadano.data.repository.IncidentRepository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class ReportsViewModel(
    private val incidentRepository: IncidentRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

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

    init {
        loadIncidents()
        loadCategories()
    }

    fun loadIncidents() {
        viewModelScope.launch {

            // Estado de carga
            _uiState.value = IncidentsUiState.Loading

            try {
                val resultI = incidentRepository.getCurrentUserIncidents()

                _allIncidents.value = resultI
                _incidents.value = resultI

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

                _allIncidents.value = emptyList()
                _incidents.value = emptyList()

                // Estado de error
                _uiState.value = IncidentsUiState.Error(
                    message = exception.message
                        ?: "Ocurrió un error al cargar los incidentes."
                )
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
                    Instant.parse(incident.createdAt)
                }
            }

            DateFilter.TODAY -> {
                val today = now
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()

                _allIncidents.value.filter { incident ->
                    Instant.parse(incident.createdAt)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate() == today
                }
            }

            DateFilter.WEEK -> {
                val limit = now.minus(7, ChronoUnit.DAYS)

                _allIncidents.value.filter { incident ->
                    Instant.parse(incident.createdAt)
                        .isAfter(limit)
                }
            }

            DateFilter.MONTH -> {
                val limit = now.minus(30, ChronoUnit.DAYS)

                _allIncidents.value.filter { incident ->
                    Instant.parse(incident.createdAt)
                        .isAfter(limit)
                }
            }

            DateFilter.YEAR -> {
                val limit = now.minus(365, ChronoUnit.DAYS)

                _allIncidents.value.filter { incident ->
                    Instant.parse(incident.createdAt)
                        .isAfter(limit)
                }
            }
        }
    }

    fun filterIncidents(
        category: Category?,
        dateFilter: DateFilter
    ) {
        var filtered = _allIncidents.value

        // Filtrar por categoría
        if (category != null) {
            filtered = filtered.filter { incident ->
                incident.category.id == category.id
            }
        }

        // Filtrar por fecha
        val now = Instant.now()

        filtered = when (dateFilter) {

            DateFilter.RECENT -> {
                filtered.sortedByDescending { incident ->
                    Instant.parse(incident.createdAt)
                }
            }

            DateFilter.TODAY -> {
                val today = now
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()

                filtered.filter { incident ->
                    Instant.parse(incident.createdAt)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate() == today
                }
            }

            DateFilter.WEEK -> {
                val limit = now.minus(7, ChronoUnit.DAYS)

                filtered.filter { incident ->
                    Instant.parse(incident.createdAt)
                        .isAfter(limit)
                }
            }

            DateFilter.MONTH -> {
                val limit = now.minus(30, ChronoUnit.DAYS)

                filtered.filter { incident ->
                    Instant.parse(incident.createdAt)
                        .isAfter(limit)
                }
            }

            DateFilter.YEAR -> {
                val limit = now.minus(365, ChronoUnit.DAYS)

                filtered.filter { incident ->
                    Instant.parse(incident.createdAt)
                        .isAfter(limit)
                }
            }
        }

        _incidents.value = filtered
    }
}