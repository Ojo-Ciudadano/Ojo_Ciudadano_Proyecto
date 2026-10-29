package cl.uct.ojociudadano.ui.login

import cl.uct.ojociudadano.ui.components.CitizenAuthLayout
import cl.uct.ojociudadano.ui.components.CitizenMessage
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.material3.TextButton

object LoginTestTags {
    const val EMAIL_FIELD = "login_email_field"
    const val PASSWORD_FIELD = "login_password_field"
    const val SUBMIT_BUTTON = "login_submit_button"
    const val LOADING_INDICATOR = "login_loading_indicator"
    const val AUTH_ERROR_MESSAGE = "login_auth_error_message"
    const val AUTH_SUCCESS_MESSAGE = "login_auth_success_message"
}

/**
 * Pantalla de inicio de sesión aislada de la capa de autenticación.
 *
 * El callback se invoca solo si las validaciones locales son satisfactorias. La pantalla no
 * realiza peticiones de red ni modifica el estado de sesión.
 */
@Composable
fun LoginScreen(
    onLogin: (LoginCredentials) -> Unit,
    authState: AuthUiState = AuthUiState.Idle,
    modifier: Modifier = Modifier,
    formValidator: LoginFormValidator = remember { LoginFormValidator() },
    onRegister: (() -> Unit)? = null,
    registrationSucceeded: Boolean = false
) {
    var formState by rememberSaveable(stateSaver = LoginFormState.Saver) {
        mutableStateOf(LoginFormState())
    }

    fun submitLogin() {
        val validatedState = formValidator.validate(formState)
        formState = validatedState

        if (!validatedState.hasValidationErrors()) {
            onLogin(
                LoginCredentials(
                    email = validatedState.email,
                    password = validatedState.password
                )
            )
        }
    }

    LoginScreenContent(
        state = formState,
        onEmailChange = { formState = formState.updateEmail(it) },
        onPasswordChange = { formState = formState.updatePassword(it) },
        onPasswordVisibilityToggle = {
            formState = formState.togglePasswordVisibility()
        },
        onLoginClick = ::submitLogin,
        authState = authState,
        modifier = modifier,
        onRegister = onRegister,
        registrationSucceeded = registrationSucceeded
    )
}

@Composable
private fun LoginScreenContent(
    state: LoginFormState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    onLoginClick: () -> Unit,
    authState: AuthUiState,
    modifier: Modifier = Modifier,
    onRegister: (() -> Unit)? = null,
    registrationSucceeded: Boolean = false
) {
    val passwordFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val isLoading = authState is AuthUiState.Loading

    fun attemptLogin() {
        keyboardController?.hide()
        focusManager.clearFocus()
        onLoginClick()
    }

    CitizenAuthLayout(modifier) {
        Spacer(modifier = Modifier.size(12.dp))

        Text(
            text = "Iniciar sesión",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Ingresa con tu correo electrónico y contraseña.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth()
        )

        AnimatedVisibility(registrationSucceeded) {
            CitizenMessage("Cuenta creada. Inicia sesión con tus credenciales.")
        }

        AnimatedContent(targetState = authState, label = "login_status") { currentState ->
        when (val authState = currentState) {
            AuthUiState.Idle -> Unit
            AuthUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.testTag(LoginTestTags.LOADING_INDICATOR)
                )
                Text("Iniciando sesión…")
            }

            is AuthUiState.Success -> {
                Text(
                    text = "Sesión iniciada.",
                    modifier = Modifier.testTag(LoginTestTags.AUTH_SUCCESS_MESSAGE)
                )
            }

            is AuthUiState.Error -> {
                CitizenMessage(authState.error.message, isError = true,
                    modifier = Modifier.testTag(LoginTestTags.AUTH_ERROR_MESSAGE))
            }
        }

        }

        OutlinedTextField(
            value = state.email,
            onValueChange = onEmailChange,
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .testTag(LoginTestTags.EMAIL_FIELD),
            label = { Text("Correo electrónico") },
            singleLine = true,
            enabled = !isLoading,
            isError = state.emailError != null,
            leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { passwordFocusRequester.requestFocus() }
            ),
            supportingText = state.emailError?.let { error -> { Text(error) } }
        )

        OutlinedTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(passwordFocusRequester)
                .animateContentSize()
                .testTag(LoginTestTags.PASSWORD_FIELD),
            label = { Text("Contraseña") },
            singleLine = true,
            enabled = !isLoading,
            isError = state.passwordError != null,
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = onPasswordVisibilityToggle) {
                    val contentDescription = if (state.isPasswordVisible) {
                        "Ocultar contraseña"
                    } else {
                        "Mostrar contraseña"
                    }
                    Icon(
                        imageVector = if (state.isPasswordVisible) {
                            Icons.Filled.VisibilityOff
                        } else {
                            Icons.Filled.Visibility
                        },
                        contentDescription = contentDescription
                    )
                }
            },
            visualTransformation = if (state.isPasswordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { attemptLogin() }),
            supportingText = state.passwordError?.let { error -> { Text(error) } }
        )

        Button(
            onClick = ::attemptLogin,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(LoginTestTags.SUBMIT_BUTTON)
        ) {
            Text("Iniciar sesión")
        }
        onRegister?.let { register ->
            TextButton(onClick = register, enabled = !isLoading) {
                Text("Crear cuenta")
            }
        }
    }
}
