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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/** Priority levels accepted by `PATCH /api/v1/incidentes/:id/prioridad` (#124). */
internal val ADMIN_PRIORITY_LEVELS = 1..5

data class AdminIncidentDetailState(
    val incidentId: String?,
    val status: IncidentsUiState = IncidentsUiState.Loading,
    val incident: Incident? = null,
    val canRetry: Boolean = false,
    val priorityDialogVisible: Boolean = false,
    val selectedPriority: Int? = null,
    val savingPriority: Boolean = false,
    val priorityError: String? = null,
    val priorityCanRetry: Boolean = false,
    val priorityUpdated: Boolean = false
) {
    val currentStatus get() = incident?.status
    val currentPriority get() = incident?.priorityLevel

    /** A write is only possible with a loaded incident and a different valid level. */
    val canConfirmPriority: Boolean
        get() = status is IncidentsUiState.Success &&
            incident != null &&
            !savingPriority &&
            selectedPriority != null &&
            selectedPriority != currentPriority &&
            (priorityError == null || priorityCanRetry)
}

class AdminIncidentDetailViewModel(
    private val repository: IncidentRepository,
    private val id: String?
) : ViewModel() {
    private val _state = MutableStateFlow(AdminIncidentDetailState(id))
    val state = _state.asStateFlow()
    private var loadJob: Job? = null
    private var priorityJob: Job? = null

    init { load() }

    fun retry() {
        if (_state.value.canRetry) load()
    }

    fun openPriorityDialog() {
        val current = _state.value
        if (current.status !is IncidentsUiState.Success || current.incident == null) return
        if (current.savingPriority) return
        _state.update {
            it.copy(
                priorityDialogVisible = true,
                selectedPriority = null,
                priorityError = null,
                priorityCanRetry = false,
                priorityUpdated = false
            )
        }
    }

    fun dismissPriorityDialog() {
        if (_state.value.savingPriority) return
        _state.update {
            it.copy(
                priorityDialogVisible = false,
                selectedPriority = null,
                priorityError = null,
                priorityCanRetry = false
            )
        }
    }

    fun selectPriority(level: Int) {
        val current = _state.value
        if (level !in ADMIN_PRIORITY_LEVELS || current.savingPriority) return
        _state.update {
            it.copy(selectedPriority = level, priorityError = null, priorityCanRetry = false)
        }
    }

    fun confirmPriority() {
        val current = _state.value
        val level = current.selectedPriority ?: return
        if (!current.canConfirmPriority) return
        if (priorityJob?.isActive == true) return
        val incident = current.incident ?: return
        if (level !in ADMIN_PRIORITY_LEVELS) return
        priorityJob = viewModelScope.launch {
            _state.update {
                it.copy(savingPriority = true, priorityError = null, priorityCanRetry = false, priorityUpdated = false)
            }
            try {
                val confirmed = repository.updatePriority(incident.id, level)
                // The PATCH response omits history and the active assignment, so keep the
                // loaded incident and only adopt the confirmed priority.
                _state.update { snapshot ->
                    val base = snapshot.incident ?: incident
                    snapshot.copy(
                        incident = base.copy(priorityLevel = confirmed.priorityLevel ?: level),
                        priorityDialogVisible = false,
                        selectedPriority = null,
                        savingPriority = false,
                        priorityError = null,
                        priorityCanRetry = false,
                        priorityUpdated = true
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _state.update {
                    it.copy(
                        savingPriority = false,
                        priorityError = exception.priorityMessage(),
                        priorityCanRetry = exception.isRetryable()
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
}

private fun Throwable.isRetryable(): Boolean = when (this) {
    is SessionRequiredException -> false
    is ApiException.Http -> statusCode in 500..599 || statusCode == 408 || statusCode == 429
    is ApiException.Network, is IOException -> true
    else -> false
}

private fun Throwable.priorityMessage(): String = when (this) {
    is SessionRequiredException -> message.orEmpty()
    is ApiException.Http -> when (statusCode) {
        400 -> "El nivel de prioridad seleccionado no es válido."
        401 -> "Tu sesión expiró. Inicia sesión nuevamente."
        403 -> "No tienes autorización para modificar la prioridad."
        404 -> "El incidente ya no está disponible."
        408, 429 -> "El servidor está ocupado. Intenta nuevamente en unos segundos."
        in 500..599 -> "El servicio de incidentes no está disponible (HTTP $statusCode). Intenta más tarde."
        else -> "No se pudo actualizar la prioridad (HTTP $statusCode). Intenta nuevamente."
    }
    is ApiException.Network, is IOException -> "No fue posible conectar de forma segura con el servidor. Revisa tu conexión e intenta nuevamente."
    else -> "La respuesta del servidor no se pudo interpretar. Intenta nuevamente."
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
