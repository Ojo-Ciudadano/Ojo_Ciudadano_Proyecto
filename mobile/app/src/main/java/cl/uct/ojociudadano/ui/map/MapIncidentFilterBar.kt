package cl.uct.ojociudadano.ui.map

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Controles de filtrado de la pantalla de mapa.
 *
 * Las opciones provienen de los incidentes ya cargados y cada selección solo
 * actualiza el estado local del [MapViewModel]; no se consulta nuevamente al
 * backend.
 */
@Composable
internal fun MapIncidentFilterBar(
    filters: MapIncidentFilters,
    categoryOptions: List<MapFilterOption>,
    statusOptions: List<MapFilterOption>,
    visibleCount: Int,
    totalCount: Int,
    onCategorySelected: (String?) -> Unit,
    onStatusSelected: (String?) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(MapTestTags.FILTERS),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 4.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Filtros del mapa",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$visibleCount de $totalCount incidentes visibles",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag(MapTestTags.FILTER_VISIBLE_COUNT)
                    )
                }
                TextButton(
                    onClick = onReset,
                    enabled = !filters.isEmpty,
                    modifier = Modifier.testTag(MapTestTags.FILTER_RESET)
                ) {
                    Text("Restablecer")
                }
            }

            if (categoryOptions.isNotEmpty()) {
                MapFilterChipRow(
                    label = "Categoría",
                    allLabel = "Todas",
                    options = categoryOptions,
                    selectedValue = filters.categoryId,
                    testTagPrefix = MapTestTags.FILTER_CATEGORY_PREFIX,
                    onValueSelected = onCategorySelected
                )
            }

            if (statusOptions.isNotEmpty()) {
                MapFilterChipRow(
                    label = "Estado",
                    allLabel = "Todos",
                    options = statusOptions,
                    selectedValue = filters.statusId,
                    testTagPrefix = MapTestTags.FILTER_STATUS_PREFIX,
                    onValueSelected = onStatusSelected
                )
            }
        }
    }
}

@Composable
private fun MapFilterChipRow(
    label: String,
    allLabel: String,
    options: List<MapFilterOption>,
    selectedValue: String?,
    testTagPrefix: String,
    onValueSelected: (String?) -> Unit
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
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedValue == null,
                onClick = { onValueSelected(null) },
                label = { Text(allLabel) },
                modifier = Modifier.testTag("${testTagPrefix}all")
            )
            options.forEach { option ->
                val selected = option.value == selectedValue
                FilterChip(
                    selected = selected,
                    onClick = {
                        onValueSelected(if (selected) null else option.value)
                    },
                    label = { Text(option.label) },
                    modifier = Modifier.testTag(testTagPrefix + option.value)
                )
            }
        }
    }
}
