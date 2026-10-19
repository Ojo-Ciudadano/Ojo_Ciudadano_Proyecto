package cl.uct.ojociudadano.ui.startup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.uct.ojociudadano.data.repository.AuthRepository
import cl.uct.ojociudadano.data.session.SessionBootstrapState
import cl.uct.ojociudadano.data.session.SessionBootstrapper
import cl.uct.ojociudadano.navigation.AppNavigation
import cl.uct.ojociudadano.navigation.AppRoutes
import cl.uct.ojociudadano.ui.login.AuthViewModel
import cl.uct.ojociudadano.ui.login.AuthViewModelFactory
import cl.uct.ojociudadano.ui.login.LoginRoute
import kotlinx.coroutines.launch

@Composable
fun SessionStartupGate(
    sessionBootstrapper: SessionBootstrapper,
    authRepository: AuthRepository
) {
    val state by sessionBootstrapper.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var isManualLoginRequested by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(sessionBootstrapper) {
        sessionBootstrapper.bootstrap()
    }

    when (state) {
        SessionBootstrapState.Loading -> StartupLoading()
        is SessionBootstrapState.Authenticated -> AppNavigation()
        is SessionBootstrapState.RequiresLogin -> LoginNavigation(
            sessionBootstrapper = sessionBootstrapper,
            authRepository = authRepository
        )
        is SessionBootstrapState.Error -> {
            if (isManualLoginRequested) {
                LoginNavigation(
                    sessionBootstrapper = sessionBootstrapper,
                    authRepository = authRepository
                )
            } else {
                StartupError(
                    onRetry = {
                        coroutineScope.launch {
                            sessionBootstrapper.bootstrap()
                        }
                    },
                    onManualLogin = { isManualLoginRequested = true }
                )
            }
        }
    }
}

@Composable
private fun LoginNavigation(
    sessionBootstrapper: SessionBootstrapper,
    authRepository: AuthRepository
) {
    val coroutineScope = rememberCoroutineScope()
    val authViewModel: AuthViewModel = viewModel(
        factory = remember(authRepository) {
            AuthViewModelFactory(authRepository)
        }
    )

    AppNavigation(
        startDestination = AppRoutes.LOGIN,
        loginContent = {
            LoginRoute(
                viewModel = authViewModel,
                onAuthenticated = {
                    coroutineScope.launch {
                        // The repository has already applied its established session contract.
                        // Bootstrap only reflects that result in the existing global startup state.
                        sessionBootstrapper.bootstrap()
                    }
                }
            )
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
private fun StartupError(
    onRetry: () -> Unit,
    onManualLogin: () -> Unit
) {
    StartupContainer {
        Text(
            text = "No fue posible restaurar la sesión",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Puedes reintentar o ingresar tus credenciales manualmente.",
            style = MaterialTheme.typography.bodyMedium
        )
        Button(onClick = onRetry) {
            Text("Reintentar")
        }
        Button(onClick = onManualLogin) {
            Text("Ir al inicio de sesión")
        }
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
