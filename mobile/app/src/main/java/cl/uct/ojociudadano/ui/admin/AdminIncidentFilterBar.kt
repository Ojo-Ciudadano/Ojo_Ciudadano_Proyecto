package cl.uct.ojociudadano.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.uct.ojociudadano.data.model.IncidentFilters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminIncidentFilterBar(
    state: AdminIncidentsState,
    onEstadoSelected: (String?) -> Unit,
    onComunaSelected: (String?) -> Unit,
    onCategoriaSelected: (String?) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filters = state.filters
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AdminFilterDropdown(
                label = "Estado",
                options = state.availableStates.map { FilterOption(it.id, it.name) },
                selectedValue = filters.estadoId,
                onValueSelected = onEstadoSelected,
                modifier = Modifier.weight(1f)
            )
            AdminFilterDropdown(
                label = "Categoría",
                options = state.categories.map { FilterOption(it.id, it.name) },
                selectedValue = filters.categoriaId,
                onValueSelected = onCategoriaSelected,
                modifier = Modifier.weight(1f)
            )
        }
        AdminFilterDropdown(
            label = "Comuna",
            options = state.availableCommunes.map { FilterOption(it, it) },
            selectedValue = filters.comuna,
            onValueSelected = onComunaSelected,
            modifier = Modifier.fillMaxWidth()
        )

        if (!filters.isEmpty) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Filtros activos", style = MaterialTheme.typography.labelLarge)
                    TextButton(onClick = onClearFilters) { Text("Limpiar filtros") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    filters.estadoId?.let { id ->
                        val label = state.availableStates.firstOrNull { it.id == id }?.name ?: "Estado"
                        ActiveFilterChip("Estado: $label") { onEstadoSelected(null) }
                    }
                    filters.categoriaId?.let { id ->
                        val label = state.categories.firstOrNull { it.id == id }?.name ?: "Categoría"
                        ActiveFilterChip("Categoría: $label") { onCategoriaSelected(null) }
                    }
                    filters.comuna?.let { comuna ->
                        ActiveFilterChip("Comuna: $comuna") { onComunaSelected(null) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActiveFilterChip(label: String, onRemove: () -> Unit) {
    FilterChip(
        selected = true,
        onClick = onRemove,
        label = { Text(label) },
        trailingIcon = {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Quitar filtro",
                modifier = Modifier.size(FilterChipDefaults.IconSize)
            )
        }
    )
}

private data class FilterOption(val value: String, val label: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminFilterDropdown(
    label: String,
    options: List<FilterOption>,
    selectedValue: String?,
    onValueSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = options.firstOrNull { it.value == selectedValue }?.label ?: "Todos",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Todos") },
                onClick = {
                    onValueSelected(null)
                    expanded = false
                }
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onValueSelected(option.value)
                        expanded = false
                    }
                )
            }
        }
    }
}
