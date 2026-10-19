package cl.uct.ojociudadano.ui.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cl.uct.ojociudadano.data.session.AuthenticatedSession

/** Connects the validated form event from [LoginScreen] to [AuthViewModel]. */
@Composable
fun LoginRoute(
    viewModel: AuthViewModel,
    onAuthenticated: (AuthenticatedSession) -> Unit,
    modifier: Modifier = Modifier
) {
    val authState by viewModel.uiState.collectAsState()
    var canHandleSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.clearStaleSuccess()
        canHandleSuccess = true
    }

    LaunchedEffect(authState, canHandleSuccess) {
        val success = authState as? AuthUiState.Success
        if (canHandleSuccess && success != null) {
            onAuthenticated(success.session)
        }
    }

    LoginScreen(
        onLogin = viewModel::login,
        authState = authState,
        modifier = modifier
    )
}
