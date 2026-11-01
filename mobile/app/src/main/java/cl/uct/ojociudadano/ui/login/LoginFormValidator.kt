package cl.uct.ojociudadano.ui.login

/** Aplica únicamente las validaciones locales requeridas antes de emitir el evento de login. */
class LoginFormValidator {

    fun validate(state: LoginFormState): LoginFormState {
        return state.copy(
            emailError = state.email.emailValidationError(),
            passwordError = state.password.requiredFieldError("La contraseña es obligatoria")
        )
    }
}

private fun String.emailValidationError(): String? = when {
    isBlank() -> "El correo electrónico es obligatorio"
    !EMAIL_PATTERN.matches(trim()) -> "Ingresa un correo electrónico válido"
    else -> null
}

private fun String.requiredFieldError(errorMessage: String): String? =
    if (isBlank()) errorMessage else null

private val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
