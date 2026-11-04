package cl.uct.ojociudadano.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cl.uct.ojociudadano.data.model.Incident
import cl.uct.ojociudadano.data.remote.error.ApiException
import cl.uct.ojociudadano.data.repository.IncidentRepository
import cl.uct.ojociudadano.data.session.SessionRequiredException
import cl.uct.ojociudadano.ui.reports.IncidentsUiState
import cl.uct.ojociudadano.ui.reports.incidentMessage
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

data class AdminIncidentDetailState(
    val incidentId: String?,
    val status: IncidentsUiState = IncidentsUiState.Loading,
    val incident: Incident? = null,
    val canRetry: Boolean = false
) {
    val currentStatus get() = incident?.status
}

class AdminIncidentDetailViewModel(
    private val repository: IncidentRepository,
    private val id: String?
) : ViewModel() {
    private val _state = MutableStateFlow(AdminIncidentDetailState(id))
    val state = _state.asStateFlow()
    private var loadJob: Job? = null

    init { load() }

    fun retry() {
        if (_state.value.canRetry) load()
    }

    private fun load() {
        if (loadJob?.isActive == true) return
        // Match the canonical UUID contract without sending missing or malformed IDs.
        if (id == null || !runCatching {
                UUID.fromString(id).toString().equals(id, ignoreCase = true)
            }.getOrDefault(false)
        ) {
            _state.value = AdminIncidentDetailState(
                id, IncidentsUiState.Error("El identificador del incidente no es válido. Vuelve al listado y selecciona un incidente.")
            )
            return
        }
        loadJob = viewModelScope.launch {
            _state.value = AdminIncidentDetailState(id)
            try {
                val incident = repository.getIncidentById(id)
                _state.value = AdminIncidentDetailState(id, IncidentsUiState.Success, incident)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _state.value = AdminIncidentDetailState(
                    id,
                    IncidentsUiState.Error(exception.incidentMessage()),
                    canRetry = when (exception) {
                        is SessionRequiredException -> false
                        is ApiException.Http -> exception.statusCode in 500..599 ||
                            exception.statusCode in setOf(408, 429)
                        is ApiException.Network, is IOException -> true
                        else -> false
                    }
                )
            }
        }
    }
}

class AdminIncidentDetailViewModelFactory(
    private val repository: IncidentRepository,
    private val id: String?
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminIncidentDetailViewModel::class.java)) {
            return AdminIncidentDetailViewModel(repository, id) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
