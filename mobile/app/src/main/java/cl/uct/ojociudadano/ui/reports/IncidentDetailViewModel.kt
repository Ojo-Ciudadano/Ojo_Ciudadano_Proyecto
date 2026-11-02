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

class IncidentDetailViewModel(private val repository: IncidentRepository, private val id: String) : ViewModel() {
    private val _state = MutableStateFlow(IncidentDetailState())
    val state = _state.asStateFlow()
    init { load() }
    fun load() {
        viewModelScope.launch {
            _state.value = IncidentDetailState()
            try {
                _state.value = IncidentDetailState(repository.getIncidentById(id), IncidentsUiState.Success)
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { _state.value = IncidentDetailState(status = IncidentsUiState.Error(e.incidentMessage())) }
        }
    }
}
