package cl.uct.ojociudadano.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cl.uct.ojociudadano.navigation.AdminFutureSection
import cl.uct.ojociudadano.ui.components.CitizenHeader

internal val AdminFutureSection.icon get() = when (this) {
    AdminFutureSection.USERS -> Icons.Default.Groups
    AdminFutureSection.STATISTICS -> Icons.Default.BarChart
    AdminFutureSection.REPORTS -> Icons.AutoMirrored.Filled.Assignment
    AdminFutureSection.MODERATION -> Icons.Default.VerifiedUser
    AdminFutureSection.SETTINGS -> Icons.Default.Settings
}

@Composable
fun AdminComingSoonScreen(section: AdminFutureSection?, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        CitizenHeader(section?.title ?: "Sección no disponible", "Administrador", onBack = onBack)
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)
        ) {
            Icon(section?.icon ?: Icons.Default.Settings, contentDescription = null,
                modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Próximamente", style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            section?.let { Text(it.description, textAlign = TextAlign.Center) }
            Text("Esta funcionalidad estará disponible en una futura actualización de Ojo Ciudadano.",
                textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onBack) { Text("Volver al panel") }
        }
    }
}
