package cl.uct.ojociudadano.ui.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

/** Connects the validated form event from [LoginScreen] to [AuthViewModel]. */
@Composable
fun LoginRoute(
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier,
    onRegister: (() -> Unit)? = null,
    registrationSucceeded: Boolean = false
) {
    val authState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.clearStaleSuccess()
    }

    LoginScreen(
        onLogin = viewModel::login,
        authState = authState,
        modifier = modifier,
        onRegister = onRegister,
        registrationSucceeded = registrationSucceeded
    )
}
