package cl.uct.ojociudadano.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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

/** Priority levels accepted by `PATCH /api/v1/incidentes/:id/prioridad` (#124). */
internal val ADMIN_PRIORITY_LEVELS = 1..5

data class AdminIncidentDetailState(
    val incidentId: String?,
    val status: IncidentsUiState = IncidentsUiState.Loading,
    val incident: Incident? = null,
    val canRetry: Boolean = false,
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
    val historyCanRetry: Boolean = false,
    val priorityDialogVisible: Boolean = false,
    val selectedPriority: Int? = null,
    val savingPriority: Boolean = false,
    val priorityError: String? = null,
    val priorityCanRetry: Boolean = false,
    val priorityUpdated: Boolean = false
) {
    val currentStatus get() = incident?.status
    val availableTransitions get() = if (actionsBlocked || syncError != null) emptyList()
        else IncidentStatusTransitions.availableFrom(currentStatus?.name.orEmpty())
    val busy get() = submitting || synchronizing || savingPriority
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
    private var historyJob: Job? = null
    private var priorityJob: Job? = null

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
