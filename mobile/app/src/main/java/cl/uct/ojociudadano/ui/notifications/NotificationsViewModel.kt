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

    /** Colección tal como la entrega la capa de datos; no se reordena en la UI. */
    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications = _notifications.asStateFlow()

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
                val rows = repository.getNotifications()
                _notifications.value = rows
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
