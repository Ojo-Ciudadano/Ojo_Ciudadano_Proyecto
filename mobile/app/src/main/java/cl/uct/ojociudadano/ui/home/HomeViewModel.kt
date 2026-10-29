package cl.uct.ojociudadano.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import cl.uct.ojociudadano.data.model.Incident
import cl.uct.ojociudadano.data.repository.IncidentRepository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

import cl.uct.ojociudadano.ui.reports.IncidentsUiState
import cl.uct.ojociudadano.ui.reports.incidentMessage

class HomeViewModel(
    private val incidentRepository: IncidentRepository
) : ViewModel() {
    private val _incidents = MutableStateFlow<List<Incident>>(emptyList())
    val incidents = _incidents.asStateFlow()

    private val _uiState = MutableStateFlow<IncidentsUiState>(IncidentsUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        loadIncidents()
    }

    fun loadIncidents() {
        viewModelScope.launch {
            _uiState.value = IncidentsUiState.Loading
            try {
                val result = incidentRepository.getCurrentUserIncidents()
                _incidents.value = result
                _uiState.value = if (result.isEmpty()) IncidentsUiState.Empty else IncidentsUiState.Success
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _incidents.value = emptyList()
                _uiState.value = IncidentsUiState.Error(exception.incidentMessage())
                runCatching {
                    Log.e(
                        "HomeViewModel",
                        "Error al cargar incidentes: type=${exception::class.java.name}, " +
                            "message=${exception.message}, cause=${exception.cause}",
                        exception
                    )
                }
            }
        }
    }
}