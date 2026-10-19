package cl.uct.ojociudadano.data.session

import cl.uct.ojociudadano.data.repository.AuthRepository
import java.util.concurrent.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionBootstrapper(
    private val sessionStorage: SessionStorage,
    private val authRepository: AuthRepository,
    private val developmentCredentials: DevelopmentAuthCredentials = DevelopmentAuthCredentials(),
    /**
     * Opt-in only for an explicit local test harness. The normal application flow must always
     * request interactive credentials when there is no valid persisted session.
     */
    private val allowDevelopmentAutoLogin: Boolean = false
) {

    private val _state = MutableStateFlow<SessionBootstrapState>(SessionBootstrapState.Loading)
    val state: StateFlow<SessionBootstrapState> = _state.asStateFlow()

    suspend fun bootstrap(): SessionBootstrapState {
        _state.value = SessionBootstrapState.Loading
        return try {
            val existingSession = sessionStorage.currentSession()

            val nextState = if (existingSession != null && !existingSession.isExpired()) {
                SessionBootstrapState.Authenticated(existingSession)
            } else {
                if (existingSession != null) {
                    sessionStorage.clear()
                }
                if (allowDevelopmentAutoLogin && developmentCredentials.isConfigured()) {
                    loginWithDevelopmentCredentials()
                } else {
                    SessionBootstrapState.RequiresLogin()
                }
            }
            _state.value = nextState
            nextState
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            SessionBootstrapState.Error(exception).also { _state.value = it }
        }
    }

    suspend fun login(email: String, password: String): SessionBootstrapState {
        val normalizedEmail = email.trim()
        if (normalizedEmail.isBlank() || password.isBlank()) {
            return SessionBootstrapState.RequiresLogin(
                message = "Ingresa tu correo y contraseña para continuar."
            ).also { _state.value = it }
        }

        _state.value = SessionBootstrapState.RequiresLogin(isSubmitting = true)

        return try {
            val session = authRepository.login(
                email = normalizedEmail,
                password = password
            )
            SessionBootstrapState.Authenticated(session).also { _state.value = it }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            SessionBootstrapState.RequiresLogin(
                message = exception.message ?: "No fue posible iniciar sesión."
            ).also { _state.value = it }
        }
    }

    fun onSessionInvalidated(
        message: String = "Tu sesión expiró. Inicia sesión nuevamente."
    ) {
        sessionStorage.clear()
        _state.value = SessionBootstrapState.RequiresLogin(message = message)
    }

    private suspend fun loginWithDevelopmentCredentials(): SessionBootstrapState {
        val session = authRepository.login(
            email = developmentCredentials.email,
            password = developmentCredentials.password
        )

        return SessionBootstrapState.Authenticated(session)
    }
}

sealed interface SessionBootstrapState {

    data object Loading : SessionBootstrapState

    data class RequiresLogin(
        val message: String? = null,
        val isSubmitting: Boolean = false
    ) : SessionBootstrapState

    data class Authenticated(
        val session: AuthenticatedSession
    ) : SessionBootstrapState

    data class Error(
        val cause: Throwable
    ) : SessionBootstrapState
}
