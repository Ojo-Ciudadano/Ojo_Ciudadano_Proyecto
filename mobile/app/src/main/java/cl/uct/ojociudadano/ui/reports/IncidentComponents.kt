package cl.uct.ojociudadano.ui.reports

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cl.uct.ojociudadano.data.model.Incident
import cl.uct.ojociudadano.ui.components.CitizenMessage
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun incidentDate(value: String): String = runCatching {
    DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm", java.util.Locale.forLanguageTag("es-CL"))
        .withZone(ZoneId.systemDefault()).format(Instant.parse(value))
}.getOrDefault("Fecha no disponible")

@Composable
fun IncidentCard(incident: Incident, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            IncidentPhoto(incident.id)
            Text(incident.category.name, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(incident.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(incident.description, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(listOfNotNull(incident.location.referenceAddress, incident.location.commune).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(incidentDate(incident.createdAt), style = MaterialTheme.typography.labelMedium)
            SuggestionChip(onClick = onClick, label = { Text(incident.status.name) })
            Text("Ver detalle", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun IncidentLoadState(state: IncidentsUiState, onRetry: () -> Unit, emptyMessage: String = "No hay incidentes en esta sección.") {
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (state) {
            IncidentsUiState.Loading -> {
                CircularProgressIndicator(Modifier.size(28.dp))
                Text("Cargando incidentes…")
            }
            IncidentsUiState.Empty -> CitizenMessage(emptyMessage)
            is IncidentsUiState.Error -> {
                CitizenMessage(state.message, isError = true)
                OutlinedButton(onClick = onRetry) { Text("Reintentar") }
            }
            IncidentsUiState.Success -> Unit
        }
    }
}
