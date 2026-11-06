package cl.uct.ojociudadano.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cl.uct.ojociudadano.data.repository.ProfileRepository
import cl.uct.ojociudadano.data.session.SessionManager
import cl.uct.ojociudadano.data.session.SessionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * Mantiene el perfil del usuario autenticado.
 *
 * Observa la sesión: cuando cambia el `userId`, cancela la consulta en curso,
 * descarta el perfil anterior y vuelve al estado de carga. Así la pantalla
 * nunca muestra datos de una sesión previa.
 */
class ProfileViewModel(
    private val repository: ProfileRepository,
    sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val state = _state.asStateFlow()

    private var loadJob: Job? = null
    private var hasObservedSession = false
    private var observedUserId: String? = null

    init {
        viewModelScope.launch {
            sessionManager.state.collect { sessionState ->
                val userId = (sessionState as? SessionState.Authenticated)?.session?.userId
                if (hasObservedSession && userId == observedUserId) return@collect
                hasObservedSession = true
                observedUserId = userId
                loadJob?.cancel()
                loadJob = null
                _state.value = ProfileUiState.Loading
                if (userId != null) load()
            }
        }
    }

    fun load() {
        // Una consulta en curso no se duplica: la pantalla puede pedir recarga
        // sin provocar dos peticiones simultáneas.
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            _state.value = ProfileUiState.Loading
            try {
                val profile = repository.getProfile()
                _state.value = ProfileUiState.Success(profile)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _state.value = ProfileUiState.Error(
                    message = exception.profileMessage(),
                    canRetry = exception.isProfileRetryable()
                )
            }
        }
    }

    fun retry() = load()

    class Factory(
        private val repository: ProfileRepository,
        private val sessionManager: SessionManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                "Unsupported ViewModel: ${modelClass.name}"
            }
            return ProfileViewModel(repository, sessionManager) as T
        }
    }
}
