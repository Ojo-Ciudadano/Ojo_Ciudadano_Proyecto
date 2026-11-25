package cl.uct.ojociudadano.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.uct.ojociudadano.ui.theme.Spacing
import cl.uct.ojociudadano.ui.theme.incidentStatusTone

/** Botón compacto que muestra u oculta la referencia visual del mapa. */
@Composable
internal fun MapLegendToggle(
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = expanded,
        onClick = onClick,
        label = { Text("Leyenda") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Map,
                contentDescription = null,
                modifier = Modifier.size(FilterChipDefaults.IconSize)
            )
        },
        modifier = modifier.testTag(MapTestTags.LEGEND_TOGGLE)
    )
}

/**
 * Referencia compacta del mapa.
 *
 * Explica las dos dimensiones del pin: el color identifica el estado y el
 * número central identifica el nivel de prioridad. Las filas usan desplazamiento
 * horizontal para mantenerse legibles en pantallas estrechas.
 */
@Composable
internal fun MapLegendPanel(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(MapTestTags.LEGEND_PANEL),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 4.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.medium, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.small)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Leyenda del mapa",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "El color indica el estado; el número, la prioridad.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag(MapTestTags.LEGEND_CLOSE)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Ocultar leyenda"
                    )
                }
            }

            MapLegendRow(label = "Estado") {
                IncidentMarkerStatus.legendEntries.forEach { status ->
                    LegendStatusItem(
                        status = status,
                        modifier = Modifier.testTag(
                            MapTestTags.LEGEND_STATUS_PREFIX + status.name
                        )
                    )
                }
            }

            MapLegendRow(label = "Prioridad") {
                IncidentMarkerPriority.legendEntries.forEach { priority ->
                    LegendPriorityItem(
                        priority = priority,
                        modifier = Modifier.testTag(
                            MapTestTags.LEGEND_PRIORITY_PREFIX + priority.name
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun MapLegendRow(
    label: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.small)
        ) {
            content()
        }
    }
}

@Composable
private fun LegendStatusItem(
    status: IncidentMarkerStatus,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.small)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(incidentStatusTone(status.backendName).content, CircleShape)
                .border(1.dp, Color.White, CircleShape)
        )
        Text(
            status.legendLabel,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

@Composable
private fun LegendPriorityItem(
    priority: IncidentMarkerPriority,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.small)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(MaterialTheme.colorScheme.surface, CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                priority.glyph,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            priority.legendLabel,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}
