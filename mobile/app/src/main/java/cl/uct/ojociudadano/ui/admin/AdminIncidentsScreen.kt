package cl.uct.ojociudadano.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.uct.ojociudadano.OjoCiudadanoApplication
import cl.uct.ojociudadano.data.model.Incident
import cl.uct.ojociudadano.ui.components.CitizenHeader
import cl.uct.ojociudadano.ui.reports.IncidentLoadState
import cl.uct.ojociudadano.ui.reports.IncidentsUiState
import cl.uct.ojociudadano.ui.reports.incidentDate

@Composable
fun AdminIncidentsScreen(onBack: () -> Unit) {
    val application = LocalContext.current.applicationContext as OjoCiudadanoApplication
    val model: AdminIncidentsViewModel = viewModel(factory = remember(application) {
        AdminIncidentsViewModelFactory(
            incidentRepository = application.container.incidentRepository,
            categoryRepository = application.container.categoryRepository
        )
    })
    val state by model.state.collectAsState()
    AdminIncidentsContent(
        state = state,
        onBack = onBack,
        onRetry = model::loadIncidents,
        onEstadoSelected = model::selectEstado,
        onComunaSelected = model::selectComuna,
        onCategoriaSelected = model::selectCategoria,
        onClearFilters = model::clearFilters
    )
}

/** #104 can supply selection by real ID once its protected administrative destination exists. */
@Composable
fun AdminIncidentsContent(
    state: AdminIncidentsState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onIncidentSelected: ((String) -> Unit)? = null,
    onEstadoSelected: (String?) -> Unit = {},
    onComunaSelected: (String?) -> Unit = {},
    onCategoriaSelected: (String?) -> Unit = {},
    onClearFilters: () -> Unit = {}
) {
    Column(modifier.fillMaxSize()) {
        CitizenHeader(title = "Incidentes", subtitle = "Administrador", onBack = onBack)
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Listado de incidentes",
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Consulta los incidentes disponibles para tu sesión administrativa.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                AdminIncidentFilterBar(
                    state = state,
                    onEstadoSelected = onEstadoSelected,
                    onComunaSelected = onComunaSelected,
                    onCategoriaSelected = onCategoriaSelected,
                    onClearFilters = onClearFilters
                )
            }
            if (state.status == IncidentsUiState.Success) {
                items(state.incidents, key = { it.id }) { incident ->
                    AdminIncidentCard(incident, onIncidentSelected)
                }
            } else {
                item {
                    IncidentLoadState(
                        state.status,
                        onRetry,
                        emptyMessage = if (state.filters.isEmpty) {
                            "Aún no hay incidentes para mostrar."
                        } else {
                            "No hay incidentes que coincidan con los filtros seleccionados."
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminIncidentCard(incident: Incident, onIncidentSelected: ((String) -> Unit)?) {
    // No dead click target or citizen detail navigation while #104 is pending.
    val selection = if (onIncidentSelected == null) Modifier else Modifier.clickable(
        role = Role.Button,
        onClickLabel = "Seleccionar incidente",
        onClick = { onIncidentSelected(incident.id) }
    )
    OutlinedCard(modifier = Modifier.fillMaxWidth().then(selection)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                incident.category.name,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                incident.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (incident.description.isNotBlank()) {
                Text(incident.description, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Text(
                    "Estado: ${incident.status.name}",
                    Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge
                )
            }
            val location = listOfNotNull(
                incident.location.referenceAddress,
                incident.location.sector,
                incident.location.commune
            ).filter { it.isNotBlank() }.distinct().joinToString(" · ")
            if (location.isNotBlank()) {
                Text(location, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                incidentDate(incident.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "ID: ${incident.id}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
