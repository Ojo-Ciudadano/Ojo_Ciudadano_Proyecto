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

    init {
        load()
    }

    fun load() {
        // Una consulta en curso no se duplica: la pantalla puede pedir recarga al
        // reanudarse sin provocar dos peticiones simultáneas.
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            _state.value = NotificationsUiState.Loading
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
                    canRetry = exception.isNotificationRetryable()
                )
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
