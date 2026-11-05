package cl.uct.ojociudadano.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cl.uct.ojociudadano.data.model.Incident
import cl.uct.ojociudadano.data.model.IncidentStatusTransitions
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
    val canRetry: Boolean = false,
    val selectedStatus: String? = null,
    val observation: String = "",
    val validationError: String? = null,
    val submitting: Boolean = false,
    val synchronizing: Boolean = false,
    val changeError: String? = null,
    val successMessage: String? = null,
    val syncError: String? = null,
    val actionsBlocked: Boolean = false
) {
    val currentStatus get() = incident?.status
    val availableTransitions get() = if (actionsBlocked || syncError != null) emptyList()
        else IncidentStatusTransitions.availableFrom(currentStatus?.name.orEmpty())
    val busy get() = submitting || synchronizing
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
