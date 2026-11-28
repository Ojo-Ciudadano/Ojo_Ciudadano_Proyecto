package cl.uct.ojociudadano.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cl.uct.ojociudadano.data.model.Notification
import cl.uct.ojociudadano.data.repository.NotificationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class NotificationsViewModel(
    private val repository: NotificationRepository
) : ViewModel() {

    private val _state = MutableStateFlow<NotificationsUiState>(NotificationsUiState.Loading)
    val state = _state.asStateFlow()

    /**
     * La colección vive en la capa de datos: la pantalla, el marcado de lectura
     * y el ciclo periódico publican sobre el mismo flujo.
     */
    val notifications = repository.notifications

    /** Conteo derivado que consumirá el indicador de no leídas (#283). */
    val unreadCount = repository.unreadCount

    private var loadJob: Job? = null

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    /** Ids con marcado individual en curso: la tarjeta muestra progreso. */
    private val _markingIds = MutableStateFlow<Set<String>>(emptySet())
    val markingIds = _markingIds.asStateFlow()

    /** Verdadero mientras el marcado masivo está en curso. */
    private val _isMarkingAll = MutableStateFlow(false)
    val isMarkingAll = _isMarkingAll.asStateFlow()

    /** Fallo de marcado apto para el ciudadano; la pantalla lo muestra descartable. */
    private val _actionError = MutableStateFlow<String?>(null)
    val actionError = _actionError.asStateFlow()

    init {
        load()
        // El ciclo periódico publica en el repositorio sin pasar por load(); el
        // estado visual debe seguir a la colección para no quedarse en Empty
        // cuando llega una notificación nueva.
        viewModelScope.launch {
            repository.notifications.collect { rows ->
                val current = _state.value
                if (current !is NotificationsUiState.Loading && current !is NotificationsUiState.Error) {
                    _state.value = if (rows.isEmpty()) {
                        NotificationsUiState.Empty
                    } else {
                        NotificationsUiState.Success
                    }
                }
            }
        }
    }

    fun refresh() = queryNotifications(refresh = true)

    fun load() = queryNotifications(refresh = false)

    private fun queryNotifications(refresh: Boolean) {
        // Una consulta en curso no se duplica: la pantalla puede pedir recarga al
        // reanudarse sin provocar dos peticiones simultáneas.
        if (loadJob?.isActive == true) return
        _isRefreshing.value = refresh
        loadJob = viewModelScope.launch {
            if (!refresh) _state.value = NotificationsUiState.Loading
            try {
                val rows = repository.refresh()
                _state.value = if (rows.isEmpty()) {
                    NotificationsUiState.Empty
                } else {
                    NotificationsUiState.Success
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _state.value = NotificationsUiState.Error(
                    message = exception.notificationMessage(),
                    canRetry = exception.isNotificationRetryable(),
                    duringRefresh = refresh
                )
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun retry() = load()

    /**
     * Abrir una notificación la marca como leída sin bloquear la navegación.
     *
     * Es best-effort: si el backend aún no expone la operación (404/405) o falla
     * la red, la notificación permanece pendiente y no se muestra un error al
     * usuario mientras navega al detalle.
     */
    fun openNotification(notification: Notification) {
        if (notification.isRead) return
        viewModelScope.launch {
            try {
                repository.markAsRead(notification.id)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                // Silencioso a propósito.
            }
        }
    }

    /**
     * Marca una notificación desde la lista y expone el fallo sin sacar al
     * usuario de la pantalla: la fila sigue visible para reintentar.
     */
    fun markAsRead(notification: Notification) {
        if (notification.isRead || notification.id in _markingIds.value) return
        _markingIds.value = _markingIds.value + notification.id
        viewModelScope.launch {
            try {
                repository.markAsRead(notification.id)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                _actionError.value = exception.notificationActionMessage()
            } finally {
                _markingIds.value = _markingIds.value - notification.id
            }
        }
    }

    /** Marca todas las pendientes con la única petición del backend (#327). */
    fun markAllAsRead() {
        if (_isMarkingAll.value) return
        _isMarkingAll.value = true
        viewModelScope.launch {
            try {
                repository.markAllAsRead()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                _actionError.value = exception.notificationActionMessage(bulk = true)
            } finally {
                _isMarkingAll.value = false
            }
        }
    }

    fun dismissActionError() {
        _actionError.value = null
    }

    class Factory(
        private val repository: NotificationRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(NotificationsViewModel::class.java)) {
                "Unsupported ViewModel: ${modelClass.name}"
            }
            return NotificationsViewModel(repository) as T
        }
    }
}
