package cl.uct.ojociudadano.ui.startup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.uct.ojociudadano.data.repository.AuthRepository
import cl.uct.ojociudadano.data.session.SessionManager
import cl.uct.ojociudadano.data.session.SessionState
import cl.uct.ojociudadano.navigation.AppNavigation
import cl.uct.ojociudadano.navigation.AppRoutes
import cl.uct.ojociudadano.ui.login.AuthViewModel
import cl.uct.ojociudadano.ui.login.AuthViewModelFactory
import cl.uct.ojociudadano.ui.login.LoginRoute
import kotlinx.coroutines.launch

@Composable
fun SessionStartupGate(
    sessionManager: SessionManager,
    authRepository: AuthRepository
) {
    val state by sessionManager.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(sessionManager) {
        sessionManager.restore()
    }

    when (state) {
        SessionState.Restoring -> StartupLoading()
        is SessionState.Authenticated -> key((state as SessionState.Authenticated).session.userId) {
            AppNavigation(
                onLogout = {
                    coroutineScope.launch {
                        runCatching { sessionManager.logout() }
                    }
                }
            )
        }
        SessionState.Unauthenticated -> LoginNavigation(
            sessionManager = sessionManager,
            authRepository = authRepository
        )
    }
}

@Composable
private fun LoginNavigation(
    sessionManager: SessionManager,
    authRepository: AuthRepository
) {
    val authViewModel: AuthViewModel = viewModel(
        factory = remember(authRepository, sessionManager) {
            AuthViewModelFactory(authRepository, sessionManager)
        }
    )

    AppNavigation(
        startDestination = AppRoutes.LOGIN,
        loginContent = {
            LoginRoute(viewModel = authViewModel)
        }
    )
}

@Composable
private fun StartupLoading() {
    StartupContainer {
        CircularProgressIndicator()
        Text("Conectando...")
    }
}

@Composable
private fun StartupContainer(
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            space = 16.dp,
            alignment = Alignment.CenterVertically
        )
    ) {
        content()
    }
}
