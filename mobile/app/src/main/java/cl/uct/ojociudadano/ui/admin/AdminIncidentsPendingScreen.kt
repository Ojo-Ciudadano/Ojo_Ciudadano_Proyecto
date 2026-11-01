package cl.uct.ojociudadano.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.uct.ojociudadano.ui.components.CitizenHeader
import cl.uct.ojociudadano.ui.components.CitizenMessage

/** Replace this destination's content when administrative incident management is implemented. */
@Composable
fun AdminIncidentsPendingScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize()) {
        CitizenHeader(title = "Gestión de incidentes", subtitle = "Administrador", onBack = onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Funcionalidad pendiente", style = MaterialTheme.typography.headlineSmall)
            CitizenMessage(
                "La gestión administrativa de incidentes aún no está disponible. " +
                    "En una próxima entrega podrás acceder al listado administrativo desde este módulo."
            )
            Text(
                "Esta pantalla no consulta incidentes ni permite realizar acciones administrativas.",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = onBack) { Text("Volver al panel") }
        }
    }
}
