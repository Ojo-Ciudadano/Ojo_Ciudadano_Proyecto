package cl.uct.ojociudadano.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cl.uct.ojociudadano.data.model.Incident
import cl.uct.ojociudadano.data.repository.IncidentRepository
import cl.uct.ojociudadano.ui.reports.IncidentsUiState
import cl.uct.ojociudadano.ui.reports.incidentMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

data class AdminIncidentsState(
    val status: IncidentsUiState = IncidentsUiState.Loading,
    val incidents: List<Incident> = emptyList()
)

class AdminIncidentsViewModel(
    private val incidentRepository: IncidentRepository
) : ViewModel() {
    private val _state = MutableStateFlow(AdminIncidentsState())
    val state = _state.asStateFlow()
    private var loadJob: Job? = null

    init {
        loadIncidents()
    }

    fun loadIncidents() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            _state.value = AdminIncidentsState()
            try {
                // General authenticated query: no reporter filter or per-row requests.
                val incidents = incidentRepository.getIncidents()
                _state.value = AdminIncidentsState(
                    status = if (incidents.isEmpty()) IncidentsUiState.Empty else IncidentsUiState.Success,
                    incidents = incidents
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _state.value = AdminIncidentsState(
                    status = IncidentsUiState.Error(exception.incidentMessage())
                )
            }
        }
    }
}

class AdminIncidentsViewModelFactory(
    private val incidentRepository: IncidentRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminIncidentsViewModel::class.java)) {
            return AdminIncidentsViewModel(incidentRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
