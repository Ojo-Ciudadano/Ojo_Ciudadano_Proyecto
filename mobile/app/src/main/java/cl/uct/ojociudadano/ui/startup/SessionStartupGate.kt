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
import cl.uct.ojociudadano.navigation.InitialDestination
import cl.uct.ojociudadano.navigation.resolveInitialDestination
import cl.uct.ojociudadano.ui.login.AuthViewModel
import cl.uct.ojociudadano.ui.login.AuthViewModelFactory
import cl.uct.ojociudadano.ui.login.LoginRoute
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import cl.uct.ojociudadano.ui.registration.RegistrationRoute
import cl.uct.ojociudadano.ui.registration.RegistrationViewModel
import cl.uct.ojociudadano.ui.registration.RegistrationViewModelFactory

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

    val destination = resolveInitialDestination(state)
    when (destination) {
        InitialDestination.LOADING -> StartupLoading()
        InitialDestination.CITIZEN,
        InitialDestination.ADMINISTRATOR,
        InitialDestination.UNAVAILABLE -> key((state as SessionState.Authenticated).session, destination) {
            // Dispose the old controller/back stack when the account, session or role changes.
            // Recomposition with the same session keeps the current destination unchanged.
            AppNavigation(
                startDestination = requireNotNull(destination.route),
                onLogout = {
                    coroutineScope.launch {
                        runCatching { sessionManager.logout() }
                    }
                }
            )
        }
        InitialDestination.AUTHENTICATION -> LoginNavigation(
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

    val navController = rememberNavController()
    var registrationSucceeded by rememberSaveable {
        mutableStateOf(false)
    }
    AppNavigation(
        navController = navController,
        startDestination = AppRoutes.LOGIN,
        loginContent = {
            LoginRoute(
                viewModel = authViewModel,
                onRegister = {
                    registrationSucceeded = false
                    navController.navigate(AppRoutes.REGISTER) { launchSingleTop = true }
                },
                registrationSucceeded = registrationSucceeded
            )
        },
        registrationContent = { onBack ->
            val registrationViewModel: RegistrationViewModel = viewModel(
                factory = remember(authRepository) {
                    RegistrationViewModelFactory(authRepository)
                }
            )
            RegistrationRoute(
                viewModel = registrationViewModel,
                onBack = onBack,
                onSuccess = {
                    registrationSucceeded = true
                    onBack()
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
