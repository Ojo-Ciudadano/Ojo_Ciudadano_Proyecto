package cl.uct.ojociudadano.ui.registration

import cl.uct.ojociudadano.data.model.RegistrationInput

data class RegistrationFieldErrors(
    val name: String? = null,
    val email: String? = null,
    val password: String? = null,
    val phone: String? = null
) {
    val hasErrors: Boolean get() = listOf(name, email, password, phone).any { it != null }
}

class RegistrationFormValidator {
    fun validate(input: RegistrationInput) = RegistrationFieldErrors(
        name = if (input.name.isBlank()) "El nombre es obligatorio" else null,
        email = when {
            input.email.isBlank() -> "El correo electrónico es obligatorio"
            !EMAIL_PATTERN.matches(input.email.trim()) -> "Ingresa un correo electrónico válido"
            else -> null
        },
        password = when {
            input.password.isBlank() -> "La contraseña es obligatoria"
            input.password.codePointCount(0, input.password.length) < 6 -> "La contraseña debe tener al menos 6 caracteres"
            else -> null
        }
        // El backend solo exige que telefono sea texto, sin formato ni longitud adicionales.
    )
}

private val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
