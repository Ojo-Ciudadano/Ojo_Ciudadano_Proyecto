package cl.uct.ojociudadano.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cl.uct.ojociudadano.data.model.CandidateUser
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
    val assignmentCompleted: Boolean = false
) {
    val currentStatus get() = incident?.status

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

    init { load() }

    fun retry() {
        if (_state.value.canRetry) load()
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
                // Best effort: resolves the responsible name and preloads the picker.
                loadCandidates()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _state.value = AdminIncidentDetailState(
                    id,
                    IncidentsUiState.Error(exception.incidentMessage()),
                    canRetry = exception.isRetryable()
                )
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

    private companion object {
        const val CANDIDATE_PAGE_SIZE = 100
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

private fun Throwable.isRetryable(): Boolean = when (this) {
    is SessionRequiredException -> false
    is ApiException.Http -> statusCode in 500..599 || statusCode in setOf(408, 429)
    is ApiException.Network, is IOException -> true
    else -> false
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
