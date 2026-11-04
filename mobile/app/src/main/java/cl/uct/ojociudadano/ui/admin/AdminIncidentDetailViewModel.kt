package cl.uct.ojociudadano.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cl.uct.ojociudadano.data.model.Incident
import cl.uct.ojociudadano.data.model.IncidentStateHistory
import cl.uct.ojociudadano.data.remote.error.ApiException
import cl.uct.ojociudadano.data.repository.IncidentRepository
import cl.uct.ojociudadano.data.session.SessionRequiredException
import cl.uct.ojociudadano.ui.reports.IncidentsUiState
import cl.uct.ojociudadano.ui.reports.incidentMessage
import java.io.IOException
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

data class AdminIncidentDetailState(
    val incidentId: String?,
    val status: IncidentsUiState = IncidentsUiState.Loading,
    val incident: Incident? = null,
    val canRetry: Boolean = false,
    val historyStatus: IncidentsUiState = IncidentsUiState.Loading,
    val history: List<IncidentStateHistory> = emptyList(),
    val historyCanRetry: Boolean = false
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
    private var historyJob: Job? = null

    init { load() }

    fun retry() {
        if (_state.value.canRetry) load()
    }

    fun retryHistory() {
        if (!_state.value.historyCanRetry) return
        val incidentId = id ?: return
        if (!hasValidId()) return
        loadHistory(incidentId)
    }

    private fun load() {
        if (loadJob?.isActive == true) return
        // Match the canonical UUID contract without sending missing or malformed IDs.
        if (!hasValidId()) {
            _state.value = AdminIncidentDetailState(
                id, IncidentsUiState.Error("El identificador del incidente no es válido. Vuelve al listado y selecciona un incidente.")
            )
            return
        }
        val incidentId = id ?: return
        _state.value = AdminIncidentDetailState(incidentId)
        loadDetail(incidentId)
        loadHistory(incidentId)
    }

    private fun loadDetail(incidentId: String) {
        loadJob = viewModelScope.launch {
            _state.update {
                it.copy(status = IncidentsUiState.Loading, incident = null, canRetry = false)
            }
            try {
                val incident = repository.getIncidentById(incidentId)
                _state.update {
                    it.copy(status = IncidentsUiState.Success, incident = incident)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _state.update {
                    it.copy(
                        status = IncidentsUiState.Error(exception.incidentMessage()),
                        incident = null,
                        canRetry = exception.isRetryable()
                    )
                }
            }
        }
    }

    /**
     * The history is fetched independently from the detail so a failure there never hides the
     * incident information. The backend already sorts ascending, but the order is normalized again
     * here to keep the timeline consistent even if the payload comes out of order.
     */
    private fun loadHistory(incidentId: String) {
        if (historyJob?.isActive == true) return
        historyJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    historyStatus = IncidentsUiState.Loading,
                    history = emptyList(),
                    historyCanRetry = false
                )
            }
            try {
                val ordered = repository.getIncidentHistory(incidentId).sortedWith(historyOrder)
                _state.update {
                    it.copy(
                        historyStatus = if (ordered.isEmpty()) {
                            IncidentsUiState.Empty
                        } else {
                            IncidentsUiState.Success
                        },
                        history = ordered
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _state.update {
                    it.copy(
                        historyStatus = IncidentsUiState.Error(exception.incidentMessage()),
                        history = emptyList(),
                        historyCanRetry = exception.isRetryable()
                    )
                }
            }
        }
    }

    private fun hasValidId(): Boolean {
        val current = id ?: return false
        return runCatching {
            UUID.fromString(current).toString().equals(current, ignoreCase = true)
        }.getOrDefault(false)
    }

    private fun Throwable.isRetryable(): Boolean = when (this) {
        is SessionRequiredException -> false
        is ApiException.Http -> statusCode in 500..599 || statusCode in setOf(408, 429)
        is ApiException.Network, is IOException -> true
        else -> false
    }

    private companion object {
        val historyOrder: Comparator<IncidentStateHistory> =
            compareBy<IncidentStateHistory, Instant?>(nullsLast()) {
                runCatching { Instant.parse(it.date) }.getOrNull()
            }.thenBy { it.id }
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
