package cl.uct.ojociudadano.ui.startup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import cl.uct.ojociudadano.data.session.SessionBootstrapState
import cl.uct.ojociudadano.data.session.SessionBootstrapper
import cl.uct.ojociudadano.navigation.AppNavigation
import kotlinx.coroutines.launch

@Composable
fun SessionStartupGate(
    sessionBootstrapper: SessionBootstrapper
) {
    val state by sessionBootstrapper.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(sessionBootstrapper) {
        sessionBootstrapper.bootstrap()
    }

    when (val currentState = state) {
        SessionBootstrapState.Loading -> StartupLoading()
        is SessionBootstrapState.Authenticated -> AppNavigation()
        is SessionBootstrapState.RequiresLogin -> StartupLogin(
            message = currentState.message,
            isSubmitting = currentState.isSubmitting,
            onLogin = { email, password ->
                coroutineScope.launch {
                    sessionBootstrapper.login(email, password)
                }
            }
        )
        is SessionBootstrapState.Error -> StartupError(
            onRetry = {
                coroutineScope.launch {
                    sessionBootstrapper.bootstrap()
                }
            },
            onManualLogin = {
                sessionBootstrapper.onSessionInvalidated(
                    message = "Inicia sesión con tus credenciales."
                )
            }
        )
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
private fun StartupLogin(
    message: String?,
    isSubmitting: Boolean,
    onLogin: (String, String) -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    StartupContainer {
        Text(
            text = "Iniciar sesión",
            style = MaterialTheme.typography.headlineSmall
        )
        if (!message.isNullOrBlank()) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo electrónico") },
            singleLine = true,
            enabled = !isSubmitting,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            singleLine = true,
            enabled = !isSubmitting,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { onLogin(email, password) },
            enabled = !isSubmitting,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isSubmitting) "Iniciando sesión..." else "Ingresar")
        }
    }
}

@Composable
private fun StartupError(
    onRetry: () -> Unit,
    onManualLogin: () -> Unit
) {
    StartupContainer {
        Text(
            text = "No se pudo iniciar la sesión automáticamente",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Verifica la conexión con el API Gateway o ingresa manualmente.",
            style = MaterialTheme.typography.bodyMedium
        )
        Button(onClick = onRetry) {
            Text("Reintentar")
        }
        TextButton(onClick = onManualLogin) {
            Text("Iniciar sesión manualmente")
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
