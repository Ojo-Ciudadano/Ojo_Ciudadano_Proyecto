package cl.uct.ojociudadano.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.uct.ojociudadano.data.model.Incident
import cl.uct.ojociudadano.data.repository.IncidentRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class IncidentDetailState(val incident: Incident? = null, val status: IncidentsUiState = IncidentsUiState.Loading)

class IncidentDetailViewModel(
    private val repository: IncidentRepository,
    id: String
) : ViewModel() {
    private val incidentId = id.trim()
    private val _state = MutableStateFlow(IncidentDetailState())
    val state = _state.asStateFlow()
    init { load() }
    fun load() {
        if (incidentId.isEmpty()) {
            _state.value = IncidentDetailState(status = IncidentsUiState.Error(INVALID_INCIDENT_ID_MESSAGE))
            return
        }
        viewModelScope.launch {
            _state.value = IncidentDetailState()
            try {
                _state.value = IncidentDetailState(
                    repository.getIncidentById(incidentId),
                    IncidentsUiState.Success
                )
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { _state.value = IncidentDetailState(status = IncidentsUiState.Error(e.incidentMessage())) }
        }
    }
}

internal const val INVALID_INCIDENT_ID_MESSAGE =
    "El identificador del incidente no es válido. Vuelve a la pantalla anterior y selecciona un incidente."
