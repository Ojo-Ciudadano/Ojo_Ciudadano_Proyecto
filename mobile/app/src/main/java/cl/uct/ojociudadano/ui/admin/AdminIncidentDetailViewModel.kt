package cl.uct.ojociudadano.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cl.uct.ojociudadano.data.model.CandidateUser
import cl.uct.ojociudadano.data.model.Incident
import cl.uct.ojociudadano.data.model.IncidentStateHistory
import cl.uct.ojociudadano.data.model.IncidentStatusTransitions
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

/** Administradores habilitados para ser responsables; [Ready] con lista vacía es un estado válido. */
sealed interface AssignmentCandidates {
    data object Loading : AssignmentCandidates
    data class Ready(val users: List<CandidateUser>, val total: Int) : AssignmentCandidates
    data class Failed(val message: String, val canRetry: Boolean) : AssignmentCandidates
}

/** Mensaje seguro y política de reintento para una operación de asignación fallida. */
data class AssignmentFailure(val message: String, val canRetry: Boolean)

data class AdminIncidentDetailState(
    val incidentId: String?,
    val status: IncidentsUiState = IncidentsUiState.Loading,
    val incident: Incident? = null,
    val canRetry: Boolean = false,
    val pickerVisible: Boolean = false,
    val candidates: AssignmentCandidates = AssignmentCandidates.Loading,
    val selectedCandidateId: String? = null,
    val assigning: Boolean = false,
    val assignmentError: AssignmentFailure? = null,
    val assignmentCompleted: Boolean = false,
    val selectedStatus: String? = null,
    val observation: String = "",
    val validationError: String? = null,
    val submitting: Boolean = false,
    val synchronizing: Boolean = false,
    val changeError: String? = null,
    val successMessage: String? = null,
    val syncError: String? = null,
    val actionsBlocked: Boolean = false,
    val historyStatus: IncidentsUiState = IncidentsUiState.Loading,
    val history: List<IncidentStateHistory> = emptyList(),
    val historyCanRetry: Boolean = false
) {
    val currentStatus get() = incident?.status

    val availableTransitions get() = if (actionsBlocked || syncError != null) emptyList()
        else IncidentStatusTransitions.availableFrom(currentStatus?.name.orEmpty())

    val busy get() = submitting || synchronizing

    /** Temporary UI selection; it is never the persisted responsible. */
    val selectedCandidate get() = (candidates as? AssignmentCandidates.Ready)
        ?.users?.firstOrNull { it.id == selectedCandidateId }

    /** Name of the confirmed responsible when the admin catalog already resolved it. */
    val responsibleName get() = incident?.activeAssignment?.let { assignment ->
        (candidates as? AssignmentCandidates.Ready)
            ?.users?.firstOrNull { it.id == assignment.assignedUserId }?.name
    }
}

class AdminIncidentDetailViewModel(
    private val repository: IncidentRepository,
    private val id: String?
) : ViewModel() {
    private val _state = MutableStateFlow(AdminIncidentDetailState(id))
    val state = _state.asStateFlow()
    private var loadJob: Job? = null
    private var candidatesJob: Job? = null
    private var assignmentJob: Job? = null
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

    fun openAssignmentPicker() {
        if (_state.value.assigning) return
        _state.update {
            it.copy(
                pickerVisible = true,
                selectedCandidateId = null,
                assignmentError = null,
                assignmentCompleted = false
            )
        }
        // Reuse the preloaded catalog; only reload after a failure or a previous empty result.
        if (_state.value.candidates !is AssignmentCandidates.Ready) loadCandidates()
    }

    fun dismissAssignmentPicker() {
        if (_state.value.assigning) return
        _state.update {
            it.copy(pickerVisible = false, selectedCandidateId = null, assignmentError = null)
        }
    }

    fun selectCandidate(candidateId: String) {
        if (_state.value.assigning) return
        _state.update { it.copy(selectedCandidateId = candidateId, assignmentError = null) }
    }

    fun retryCandidates() = loadCandidates()

    fun confirmAssignment() {
        val current = _state.value
        val incidentId = current.incidentId
        val candidateId = current.selectedCandidateId
        if (current.assigning || current.incident == null || incidentId == null || candidateId == null) return
        if (assignmentJob?.isActive == true) return
        assignmentJob = viewModelScope.launch {
            // Keep the previous responsible visible until the backend confirms the new one.
            _state.update { it.copy(assigning = true, assignmentError = null) }
            try {
                val confirmed = repository.assignResponsible(incidentId, candidateId)
                _state.update {
                    it.copy(
                        assigning = false,
                        pickerVisible = false,
                        selectedCandidateId = null,
                        assignmentCompleted = true,
                        incident = it.incident?.copy(activeAssignment = confirmed)
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _state.update {
                    it.copy(
                        assigning = false,
                        assignmentError = AssignmentFailure(
                            exception.assignmentMessage(),
                            exception.isRetryable()
                        )
                    )
                }
            }
        }
    }

    fun selectStatus(destination: String) {
        val current = _state.value
        if (current.busy || current.selectedStatus != null || destination !in current.availableTransitions) return
        _state.value = current.copy(selectedStatus = destination, observation = "", validationError = null,
            changeError = null, successMessage = null)
    }

    fun editObservation(value: String) {
        val current = _state.value
        if (current.busy || current.selectedStatus == null) return
        _state.value = current.copy(observation = value, validationError = null, changeError = null)
    }

    fun cancelChange() {
        if (_state.value.busy) return
        _state.value = _state.value.copy(selectedStatus = null, observation = "", validationError = null)
    }

    fun confirmChange() {
        val current = _state.value
        val destination = current.selectedStatus ?: return
        val incident = current.incident ?: return
        if (current.busy || destination !in current.availableTransitions) return
        if (current.observation.isBlank()) {
            _state.value = current.copy(validationError = "Ingresa una observación con contenido.")
            return
        }
        // Publish synchronously so a second confirmation cannot queue another PATCH.
        _state.value = current.copy(submitting = true, validationError = null, changeError = null)
        viewModelScope.launch {
            try {
                val confirmed = repository.updateStatus(incident.id, destination, current.observation.trim())
                check(confirmed.id == incident.id && IncidentStatusTransitions.isCanonical(confirmed.status.name))
                _state.value = _state.value.copy(incident = confirmed, submitting = false,
                    selectedStatus = null, observation = "", successMessage = "Estado actualizado correctamente.")
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _state.value = _state.value.copy(submitting = false, changeError = exception.statusChangeMessage(),
                    actionsBlocked = exception is SessionRequiredException ||
                        (exception is ApiException.Http && exception.statusCode in setOf(401, 403, 404)))
                return@launch
            }
            synchronize()
        }
    }

    fun retrySynchronization() {
        if (_state.value.busy || _state.value.syncError == null) return
        _state.value = _state.value.copy(synchronizing = true)
        viewModelScope.launch { synchronize() }
    }

    private suspend fun synchronize() {
        val incident = _state.value.incident ?: return
        _state.value = _state.value.copy(synchronizing = true, syncError = null)
        try {
            val refreshed = repository.getIncidentById(incident.id)
            check(refreshed.id == incident.id)
            _state.value = _state.value.copy(incident = refreshed, synchronizing = false)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            // PATCH already succeeded. Keep its confirmed result and retry only GET.
            _state.value = _state.value.copy(synchronizing = false,
                syncError = "El cambio fue confirmado, pero no se pudo recargar el detalle. " + exception.incidentMessage())
        }
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
        // Best effort: resolves the responsible name and preloads the picker.
        loadCandidates()
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

    private fun loadCandidates() {
        if (candidatesJob?.isActive == true) return
        candidatesJob = viewModelScope.launch {
            _state.update { it.copy(candidates = AssignmentCandidates.Loading) }
            try {
                val page = repository.getCandidates(page = 1, limit = CANDIDATE_PAGE_SIZE)
                _state.update { it.copy(candidates = AssignmentCandidates.Ready(page.items, page.total)) }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _state.update {
                    it.copy(
                        candidates = AssignmentCandidates.Failed(
                            exception.candidateMessage(),
                            exception.isRetryable()
                        )
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
        const val CANDIDATE_PAGE_SIZE = 100

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

private fun Throwable.statusChangeMessage(): String = when (this) {
    is ApiException.Http -> when (statusCode) {
        400 -> "Revisa el estado y la observación de la solicitud."
        403 -> "No tienes autorización para cambiar el estado de este incidente."
        409 -> "La transición fue rechazada. El incidente puede haber cambiado; vuelve a abrir el detalle para actualizarlo."
        else -> incidentMessage()
    }
    is ApiException.Network, is IOException -> if (this is SessionRequiredException) incidentMessage()
        else "No se pudo confirmar el resultado. Revisa tu conexión y vuelve a abrir el detalle antes de reintentar."
    else -> "No se pudo confirmar el resultado del cambio. Vuelve a abrir el detalle para consultar el estado actual."
}

/** Never expose backend diagnostics; the assignment contract uses distinct wording from queries. */
private fun Throwable.assignmentMessage(): String = when (this) {
    is SessionRequiredException -> "Tu sesión expiró. Inicia sesión nuevamente."
    is ApiException.Http -> when (statusCode) {
        400 -> "El usuario seleccionado no es un administrador válido."
        401 -> "Tu sesión expiró. Inicia sesión nuevamente."
        403 -> "No tienes autorización para asignar responsables."
        404 -> "El incidente o el usuario ya no está disponible."
        in 500..599 -> "El servicio no está disponible (HTTP $statusCode). Intenta más tarde."
        else -> "No se pudo completar la asignación (HTTP $statusCode)."
    }
    is ApiException.Network, is IOException ->
        "No fue posible conectar de forma segura con el servidor. Revisa tu conexión e intenta nuevamente."
    else -> "La respuesta del servidor no se pudo interpretar. Intenta nuevamente."
}

private fun Throwable.candidateMessage(): String = when (this) {
    is SessionRequiredException -> "Tu sesión expiró. Inicia sesión nuevamente."
    is ApiException.Http -> when (statusCode) {
        401 -> "Tu sesión expiró. Inicia sesión nuevamente."
        403 -> "No tienes autorización para consultar administradores."
        in 500..599 -> "El servicio no está disponible (HTTP $statusCode). Intenta más tarde."
        else -> "No se pudo consultar a los administradores (HTTP $statusCode)."
    }
    is ApiException.Network, is IOException ->
        "No fue posible conectar de forma segura con el servidor. Revisa tu conexión e intenta nuevamente."
    else -> "La respuesta del servidor no se pudo interpretar. Intenta nuevamente."
}
