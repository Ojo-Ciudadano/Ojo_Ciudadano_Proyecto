package cl.uct.ojociudadano.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import cl.uct.ojociudadano.data.model.IncidentStateHistory
import cl.uct.ojociudadano.ui.components.CitizenMessage
import cl.uct.ojociudadano.ui.reports.IncidentsUiState
import cl.uct.ojociudadano.ui.reports.incidentDate

/**
 * Renders the state-change history as its own card, visually separated from the current incident
 * information. Every visual state (loading, empty, error, success) is driven by the ViewModel.
 */
@Composable
internal fun AdminIncidentHistorySection(
    status: IncidentsUiState,
    history: List<IncidentStateHistory>,
    canRetry: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    DetailSection(title = "Historial de cambios", modifier = modifier) {
        when (status) {
            IncidentsUiState.Loading -> HistoryLoading()
            IncidentsUiState.Empty -> Text(
                "Este incidente aún no registra cambios de estado.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            is IncidentsUiState.Error -> {
                CitizenMessage(status.message, isError = true)
                if (canRetry) {
                    OutlinedButton(onClick = onRetry) { Text("Reintentar") }
                }
            }
            IncidentsUiState.Success -> history.forEach { entry -> HistoryEntry(entry) }
        }
    }
}

@Composable
private fun HistoryLoading() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        Text("Cargando historial…", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun HistoryEntry(entry: IncidentStateHistory) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier
                .padding(top = 6.dp)
                .size(10.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AdminIncidentStatusBadge(entry.status.name)
            Text(
                incidentDate(entry.date),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val observation = entry.observation?.takeIf { it.isNotBlank() }
            Text(
                observation ?: "Sin observación",
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = if (observation == null) FontStyle.Italic else FontStyle.Normal
            )
            Text(
                entry.userId.takeIf { it.isNotBlank() }?.let { "Usuario: $it" } ?: "Usuario no disponible",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
