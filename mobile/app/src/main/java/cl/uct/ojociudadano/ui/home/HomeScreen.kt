package cl.uct.ojociudadano.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.uct.ojociudadano.OjoCiudadanoApplication
import cl.uct.ojociudadano.data.model.Incident
import cl.uct.ojociudadano.data.session.SessionState
import cl.uct.ojociudadano.ui.components.CitizenHeader
import cl.uct.ojociudadano.ui.reports.*

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onReportsClick: () -> Unit = {},
    onCreateReportClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    recentIncidents: List<Incident>,
    incidentsUnderReview: Int,
    incidentsInViewOf: Int,
    solvedIncidents: Int,
    name: String = "",
    state: IncidentsUiState = IncidentsUiState.Success,
    onRetry: () -> Unit = {},
    onMapClick: () -> Unit = {},
    onIncidentClick: (String) -> Unit = {}
) {
    Column(modifier.fillMaxSize()) {
        CitizenHeader("Ojo Ciudadano", "Ciudadano") {
            IconButton(onClick = onNotificationsClick) { Icon(Icons.Default.Notifications, "Notificaciones", tint = androidx.compose.ui.graphics.Color.White) }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (name.isBlank()) "Hola" else "Hola, ${name.trim().substringBefore(' ')}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Juntos cuidamos nuestra ciudad.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onCreateReportClick, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Nuevo reporte", style = MaterialTheme.typography.titleMedium)
            }
            Text("Mis incidentes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            val ready = state == IncidentsUiState.Success || state == IncidentsUiState.Empty
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusCard(if (ready) "$incidentsUnderReview" else "—", "En revisión", Modifier.weight(1f))
                StatusCard(if (ready) "$incidentsInViewOf" else "—", "En atención", Modifier.weight(1f))
                StatusCard(if (ready) "$solvedIncidents" else "—", "Resueltos", Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Mis reportes recientes", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = onReportsClick) { Text("Ver todos") }
            }
            IncidentLoadState(state, onRetry, "Todavía no has registrado incidentes.")
            if (state == IncidentsUiState.Success) recentIncidents.forEach { incident ->
                IncidentCard(incident) { onIncidentClick(incident.id) }
            }
            OutlinedCard(onClick = onMapClick, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(Icons.Default.Map, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(32.dp))
                    Column {
                        Text("Mapa de incidentes", style = MaterialTheme.typography.titleMedium)
                        Text("Consultar disponibilidad", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusCard(value: String, label: String, modifier: Modifier) {
    Card(modifier, elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun HomeScreen(onReportsClick: () -> Unit = {}, onCreateReportClick: () -> Unit = {},
               onNotificationsClick: () -> Unit = {}, onMapClick: () -> Unit = {}, onIncidentClick: (String) -> Unit = {}) {
    val app = LocalContext.current.applicationContext as OjoCiudadanoApplication
    val model: HomeViewModel = viewModel(factory = remember(app) { HomeViewModelFactory(app.container.incidentRepository) })
    val incidents by model.incidents.collectAsState()
    val state by model.uiState.collectAsState()
    val session by app.container.sessionManager.state.collectAsState()
    // Refresh when returning from creation or another destination.
    LaunchedEffect(Unit) { model.loadIncidents() }
    HomeScreen(onReportsClick = onReportsClick, onCreateReportClick = onCreateReportClick,
        onNotificationsClick = onNotificationsClick, onMapClick = onMapClick, onIncidentClick = onIncidentClick,
        recentIncidents = incidents.sortedByDescending { it.createdAt }.take(3),
        incidentsUnderReview = incidents.count { it.status.name in listOf("Nuevo", "En revisión") },
        incidentsInViewOf = incidents.count { it.status.name == "En atención" },
        solvedIncidents = incidents.count { it.status.name in listOf("Resuelto", "Cerrado") },
        name = (session as? SessionState.Authenticated)?.session?.name.orEmpty(), state = state, onRetry = model::loadIncidents)
}
