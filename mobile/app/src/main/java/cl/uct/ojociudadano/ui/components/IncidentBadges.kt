package cl.uct.ojociudadano.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.uct.ojociudadano.ui.theme.IncidentTone
import cl.uct.ojociudadano.ui.theme.incidentPriorityTone
import cl.uct.ojociudadano.ui.theme.incidentStatusTone

/** Informational chips have no empty click action or misleading button semantics. */
@Composable
fun IncidentStatusBadge(name: String) {
    IncidentBadge("Estado: ${name.ifBlank { "No disponible" }}", incidentStatusTone(name))
}

@Composable
fun IncidentPriorityBadge(level: Int?) {
    val label = when (level) {
        1 -> "1 · Mínima"
        2 -> "2 · Baja"
        3 -> "3 · Media"
        4 -> "4 · Alta"
        5 -> "5 · Crítica"
        else -> "Sin prioridad registrada"
    }
    IncidentBadge("Prioridad: $label", incidentPriorityTone(level))
}

@Composable
private fun IncidentBadge(label: String, tone: IncidentTone) {
    Surface(shape = MaterialTheme.shapes.small, color = tone.container, contentColor = tone.content) {
        Text(label, Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge)
    }
}
