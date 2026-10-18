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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.uct.ojociudadano.data.session.SessionBootstrapState
import cl.uct.ojociudadano.data.session.SessionBootstrapper
import cl.uct.ojociudadano.navigation.AppNavigation
import cl.uct.ojociudadano.navigation.AppRoutes
import cl.uct.ojociudadano.ui.login.LoginCredentials
import kotlinx.coroutines.launch

@Composable
fun SessionStartupGate(
    sessionBootstrapper: SessionBootstrapper,
    onLogin: (LoginCredentials) -> Unit = {}
) {
    var state: SessionBootstrapState by remember {
        mutableStateOf(SessionBootstrapState.Loading)
    }
    var isLoginRequested by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(sessionBootstrapper) {
        state = sessionBootstrapper.bootstrap()
    }

    if (isLoginRequested) {
        AppNavigation(
            startDestination = AppRoutes.LOGIN,
            onLogin = onLogin
        )
    } else {
        when (state) {
            SessionBootstrapState.Loading -> StartupLoading()
            is SessionBootstrapState.Authenticated -> AppNavigation(onLogin = onLogin)
            is SessionBootstrapState.Error -> StartupError(
                onRetry = {
                    state = SessionBootstrapState.Loading
                    coroutineScope.launch {
                        state = sessionBootstrapper.bootstrap()
                    }
                },
                onLoginClick = { isLoginRequested = true }
            )
        }
    }
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
    onLoginClick: () -> Unit
) {
    StartupContainer {
        Text(
            text = "No se pudo iniciar la sesión de desarrollo",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Verifica la configuración local y que el API Gateway esté disponible.",
            style = MaterialTheme.typography.bodyMedium
        )
        Button(onClick = onRetry) {
            Text("Reintentar")
        }
        Button(onClick = onLoginClick) {
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
