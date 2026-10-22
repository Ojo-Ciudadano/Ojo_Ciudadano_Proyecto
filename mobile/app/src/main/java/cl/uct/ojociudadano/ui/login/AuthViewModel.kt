package cl.uct.ojociudadano.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.uct.ojociudadano.data.remote.error.ApiException
import cl.uct.ojociudadano.data.repository.AuthRepository
import cl.uct.ojociudadano.data.session.AuthenticatedSession
import cl.uct.ojociudadano.data.session.SessionManager
import java.io.IOException
import java.util.concurrent.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /**
     * Starts an interactive authentication attempt. A new attempt replaces a previous error or
     * success state with [AuthUiState.Loading], while an in-progress request is left untouched.
     */
    fun login(credentials: LoginCredentials) {
        login(email = credentials.email, password = credentials.password)
    }

    fun login(email: String, password: String) {
        if (_uiState.value is AuthUiState.Loading) {
            return
        }

        _uiState.value = AuthUiState.Loading

        viewModelScope.launch {
            try {
                val session = authRepository.login(
                    email = email.trim(),
                    password = password
                )
                sessionManager.establish(session)
                _uiState.value = AuthUiState.Success(session)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.value = AuthUiState.Error(exception.toAuthError())
            }
        }
    }

    /**
     * A ViewModel scoped above the login destination can outlive a completed login. Clear only a
     * stale success when the destination is shown again; never cancel or hide an active request.
     */
    fun clearStaleSuccess() {
        if (_uiState.value is AuthUiState.Success) {
            _uiState.value = AuthUiState.Idle
        }
    }
}

sealed interface AuthUiState {

    data object Idle : AuthUiState

    data object Loading : AuthUiState

    data class Success(
        val session: AuthenticatedSession
    ) : AuthUiState

    data class Error(
        val error: AuthError
    ) : AuthUiState
}

sealed interface AuthError {

    val message: String

    data object InvalidCredentials : AuthError {
        override val message = "El correo o la contraseña no son correctos."
    }

    data class Validation(
        override val message: String
    ) : AuthError

    data object Network : AuthError {
        override val message = "No fue posible conectar con el servidor. Intenta nuevamente."
    }

    data object Server : AuthError {
        override val message = "El servidor no pudo procesar el inicio de sesión. Intenta más tarde."
    }

    data object Unexpected : AuthError {
        override val message = "Ocurrió un error inesperado. Intenta nuevamente."
    }
}

private fun Exception.toAuthError(): AuthError = when (this) {
    is ApiException.Network,
    is IOException -> AuthError.Network

    is ApiException.Http -> when {
        statusCode == HTTP_UNAUTHORIZED -> AuthError.InvalidCredentials
        statusCode == HTTP_BAD_REQUEST || statusCode == HTTP_UNPROCESSABLE_ENTITY -> {
            AuthError.Validation(
                message = serverMessages
                    .takeIf { it.isNotEmpty() }
                    ?.joinToString(separator = "\n")
                    ?: "Revisa los datos ingresados."
            )
        }

        statusCode in HTTP_SERVER_ERROR_RANGE -> AuthError.Server
        else -> AuthError.Unexpected
    }

    else -> AuthError.Unexpected
}

private const val HTTP_BAD_REQUEST = 400
private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_UNPROCESSABLE_ENTITY = 422
private val HTTP_SERVER_ERROR_RANGE = 500..599
