package cl.uct.ojociudadano.ui.login

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver

data class LoginFormState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null
) {

    fun updateEmail(email: String): LoginFormState =
        copy(email = email, emailError = null)

    fun updatePassword(password: String): LoginFormState =
        copy(password = password, passwordError = null)

    fun togglePasswordVisibility(): LoginFormState =
        copy(isPasswordVisible = !isPasswordVisible)

    fun hasValidationErrors(): Boolean = emailError != null || passwordError != null

    companion object {
        /**
         * Conserva el formulario ante recreaciones normales de la interfaz sin persistirlo en
         * almacenamiento de sesión.
         */
        val Saver: Saver<LoginFormState, Any> = listSaver(
            save = { state ->
                listOf(
                    state.email,
                    state.password,
                    state.isPasswordVisible.toString(),
                    state.emailError.orEmpty(),
                    state.passwordError.orEmpty()
                )
            },
            restore = { values ->
                LoginFormState(
                    email = values[0],
                    password = values[1],
                    isPasswordVisible = values[2].toBoolean(),
                    emailError = values[3].ifEmpty { null },
                    passwordError = values[4].ifEmpty { null }
                )
            }
        )
    }
}
