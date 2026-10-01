package cl.uct.ojociudadano.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.uct.ojociudadano.OjoCiudadanoApplication
import cl.uct.ojociudadano.data.model.Category
import cl.uct.ojociudadano.data.model.Incident
import cl.uct.ojociudadano.data.model.IncidentLocation
import cl.uct.ojociudadano.data.model.IncidentStatus
import cl.uct.ojociudadano.ui.theme.OjoCiudadanoTheme

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onReportsClick: () -> Unit = {},
    onCreateReportClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    recentIncidents: List<Incident>,
    incidentsUnderReview: Int,
    incidentsInViewOf: Int,
    solvedIncidents: Int
) {
    Column(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        HomeHeader(onNotificationsClick)
        HomeContent(
            onCreateReportClick, onReportsClick, recentIncidents,
            incidentsUnderReview, incidentsInViewOf, solvedIncidents
        )
    }
}

@Composable
private fun HomeHeader(onNotificationsClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color(0xFF023889)).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Visibility, null, Modifier.size(32.dp), Color.White)
            Column {
                Text("Ojo ciudadano", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Tu ciudad, tu reporte", color = Color.White.copy(alpha = 0.82f), style = MaterialTheme.typography.labelMedium)
            }
        }
        IconButton(onClick = onNotificationsClick) {
            Icon(Icons.Filled.Notifications, "Notificaciones", tint = Color.White)
        }
    }
}

@Composable
private fun HomeContent(
    onCreateReportClick: () -> Unit,
    onReportsClick: () -> Unit,
    recentIncidents: List<Incident>,
    incidentsUnderReview: Int,
    incidentsInViewOf: Int,
    solvedIncidents: Int
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(20.dp)) {
                Text("¡Bienvenido!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Gracias por ayudar a mejorar nuestra ciudad.", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Button(
            onClick = onCreateReportClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Filled.AddCircle, null)
            Spacer(Modifier.size(8.dp))
            Text("Nuevo reporte", fontWeight = FontWeight.Bold)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReportStatusCard(incidentsUnderReview.toString(), "En revisión", "Pendientes", Icons.Filled.HourglassTop, Modifier.weight(1f))
            ReportStatusCard(incidentsInViewOf.toString(), "En atención", "Atendidos", Icons.Filled.ReportProblem, Modifier.weight(1f))
            ReportStatusCard(solvedIncidents.toString(), "Cerrados", "Resueltos", Icons.Filled.CheckCircle, Modifier.weight(1f))
        }
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Reportes recientes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    TextButton(onClick = onReportsClick) { Text("Ver todos") }
                }
                if (recentIncidents.isEmpty()) {
                    Text("Aún no hay reportes recientes.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    recentIncidents.forEach { RecentReportItem(it, onReportsClick) }
                }
            }
        }
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Row(
                Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(Icons.Filled.Map, null, Modifier.size(36.dp), MaterialTheme.colorScheme.primary)
                Column {
                    Text("Mapa de incidentes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Disponible próximamente", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun ReportStatusCard(value: String, title: String, description: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(14.dp)) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RecentReportItem(incident: Incident, onReportsClick: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Filled.ReportProblem, null, tint = MaterialTheme.colorScheme.error)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(incident.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(incident.description, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, null, Modifier.size(14.dp), MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(incident.createdAt, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            IconButton(onClick = onReportsClick) { Icon(Icons.Filled.ChevronRight, "Ver reporte") }
        }
    }
}

@Composable
fun HomeScreen(
    onReportsClick: () -> Unit = {},
    onCreateReportClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {}
) {
    val application = LocalContext.current.applicationContext as OjoCiudadanoApplication
    val viewModel: HomeViewModel = viewModel(
        factory = remember(application) { HomeViewModelFactory(application.container.incidentRepository) }
    )
    val incidents by viewModel.incidents.collectAsState()
    HomeScreen(
        onReportsClick = onReportsClick, onCreateReportClick = onCreateReportClick, onNotificationsClick = onNotificationsClick,
        recentIncidents = incidents.sortedByDescending { it.createdAt }.take(2),
        incidentsUnderReview = incidents.count { it.status.name == "En revisión" },
        incidentsInViewOf = incidents.count { it.status.name == "En atención" },
        solvedIncidents = incidents.count { it.status.name == "Cerrado" }
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    OjoCiudadanoTheme {
        HomeScreen(
            recentIncidents = listOf(
                Incident(
                    id = "preview", title = "Luminaria apagada", description = "El poste de luz no enciende desde hace tres días.",
                    category = Category("category", "Alumbrado público", "", 2, false, null),
                    status = IncidentStatus("status", "Nuevo", 1),
                    location = IncidentLocation("location", -33.4372, -70.6506, "Av. O'Higgins", "Santiago", "Metropolitana", "Centro"),
                    reporterUserId = "user", priorityLevel = null, createdAt = "2026-09-28", updatedAt = "2026-09-28"
                )
            ), incidentsUnderReview = 5, incidentsInViewOf = 6, solvedIncidents = 7
        )
    }
}
