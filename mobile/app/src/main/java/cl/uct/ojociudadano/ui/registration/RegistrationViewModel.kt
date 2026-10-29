package cl.uct.ojociudadano.ui.registration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cl.uct.ojociudadano.data.model.RegistrationInput
import cl.uct.ojociudadano.data.remote.error.ApiException
import cl.uct.ojociudadano.data.repository.AuthRepository
import java.io.IOException
import java.util.concurrent.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface RegistrationUiState {
    data object Idle : RegistrationUiState
    data class Invalid(val fields: RegistrationFieldErrors) : RegistrationUiState
    data object Loading : RegistrationUiState
    data object Success : RegistrationUiState
    data class Error(val message: String, val fields: RegistrationFieldErrors = RegistrationFieldErrors()) : RegistrationUiState
}

/** Separado del login: este flujo no tiene acceso a SessionManager y nunca establece sesión. */
class RegistrationViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<RegistrationUiState>(RegistrationUiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun register(input: RegistrationInput) {
        if (_uiState.value == RegistrationUiState.Loading || _uiState.value == RegistrationUiState.Success) return
        val errors = RegistrationFormValidator().validate(input)
        if (errors.hasErrors) {
            _uiState.value = RegistrationUiState.Invalid(errors)
            return
        }
        _uiState.value = RegistrationUiState.Loading
        viewModelScope.launch {
            try {
                repository.register(input)
                _uiState.value = RegistrationUiState.Success
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.value = exception.toRegistrationError()
            }
        }
    }
}

private fun Exception.toRegistrationError(): RegistrationUiState.Error = when (this) {
    is ApiException.Network, is IOException -> RegistrationUiState.Error(
        "No pudimos confirmar el registro por un problema de conexión. Si ya se creó tu cuenta, intenta iniciar sesión antes de volver a registrarte."
    )
    is ApiException.Http -> when (statusCode) {
        409 -> RegistrationUiState.Error(
            "Ya existe una cuenta con ese correo. Puedes iniciar sesión.",
            RegistrationFieldErrors(email = "Este correo ya está registrado")
        )
        400, 422 -> {
            // Identificar campos conocidos sin mostrar textos internos ni valores recibidos.
            val fields = RegistrationFieldErrors(
                name = if (serverMessages.any { it.contains("nombre", true) }) "Revisa el nombre ingresado" else null,
                email = if (serverMessages.any { it.contains("email", true) || it.contains("correo", true) }) "Revisa el correo electrónico" else null,
                password = if (serverMessages.any { it.contains("password", true) || it.contains("contras", true) }) "Revisa la contraseña (mínimo 6 caracteres)" else null,
                phone = if (serverMessages.any { it.contains("telefono", true) || it.contains("teléfono", true) }) "Revisa el teléfono ingresado" else null
            )
            RegistrationUiState.Error("El servidor rechazó los datos. Revisa el formulario.", fields)
        }
        in 500..599 -> RegistrationUiState.Error("El servidor no pudo completar el registro. Intenta más tarde.")
        else -> RegistrationUiState.Error("No pudimos confirmar el registro. Intenta iniciar sesión si tu cuenta ya fue creada.")
    }
    else -> RegistrationUiState.Error("No pudimos confirmar el registro. Intenta iniciar sesión si tu cuenta ya fue creada.")
}

class RegistrationViewModelFactory(private val repository: AuthRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(RegistrationViewModel::class.java))
        return RegistrationViewModel(repository) as T
    }
}
