package cl.uct.ojociudadano.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.uct.ojociudadano.OjoCiudadanoApplication
import cl.uct.ojociudadano.data.model.UserProfile
import cl.uct.ojociudadano.data.session.SessionManager
import cl.uct.ojociudadano.ui.components.CitizenHeader
import cl.uct.ojociudadano.ui.components.CitizenMessage
import cl.uct.ojociudadano.ui.reports.incidentDate

/**
 * Perfil del usuario autenticado.
 *
 * El `SessionManager` se recibe por parámetro (en lugar de leerlo del
 * contenedor global) para que la pantalla use exactamente la sesión del grafo
 * de navegación que la muestra.
 */
@Composable
fun ProfileScreen(
    sessionManager: SessionManager,
    modifier: Modifier = Modifier,
    onLogout: () -> Unit = {}
) {
    val application = LocalContext.current.applicationContext as OjoCiudadanoApplication
    val model: ProfileViewModel = viewModel(
        factory = remember(application, sessionManager) {
            ProfileViewModel.Factory(application.container.profileRepository, sessionManager)
        }
    )
    val state by model.state.collectAsState()
    ProfileContent(state = state, onRetry = model::retry, onLogout = onLogout, modifier = modifier)
}

@Composable
fun ProfileContent(
    state: ProfileUiState,
    onRetry: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CitizenHeader("Mi perfil")
        when (state) {
            ProfileUiState.Loading -> ProfileLoading()
            is ProfileUiState.Error -> ProfileError(state, onRetry)
            is ProfileUiState.Success -> {
                ProfileIdentity(state.profile)
                ProfileDetails(state.profile)
            }
        }
        SettingsCard()
        // La acción de sesión no depende de los datos del perfil: se mantiene
        // disponible incluso mientras la consulta carga o falla.
        Button(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
            Text("Cerrar sesión", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun ProfileLoading() {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
            Text("Cargando tu perfil…", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ProfileError(state: ProfileUiState.Error, onRetry: () -> Unit) {
    CitizenMessage(state.message, isError = true)
    if (state.canRetry) {
        OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Text("Reintentar")
        }
    }
}

@Composable
private fun ProfileIdentity(profile: UserProfile) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    profile.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(profile.email, style = MaterialTheme.typography.bodyMedium)
                Text(
                    roleLabel(profile.roles),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ProfileDetails(profile: UserProfile) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Datos de la cuenta", style = MaterialTheme.typography.titleMedium)
            ProfileDetail("Teléfono", profile.phone?.takeIf { it.isNotBlank() } ?: "No registrado")
            ProfileDetail("Miembro desde", profile.registeredAt?.let(::incidentDate) ?: "No disponible")
            ProfileDetail("Rol", roleLabel(profile.roles))
        }
    }
}

@Composable
private fun ProfileDetail(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SettingsCard() {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Filled.Settings, contentDescription = null)
            Text(
                "La apariencia y el movimiento respetan las preferencias del sistema.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun roleLabel(roles: List<String>): String = when {
    roles.isEmpty() -> "Sin rol asignado"
    else -> roles.joinToString(", ") { role ->
        when (role) {
            "ADMIN" -> "Administrador"
            "CIUDADANO" -> "Ciudadano"
            else -> role
        }
    }
}
