package cl.uct.ojociudadano.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.uct.ojociudadano.OjoCiudadanoApplication
import cl.uct.ojociudadano.data.model.Incident
import cl.uct.ojociudadano.ui.components.CitizenHeader
import cl.uct.ojociudadano.ui.components.CitizenMessage
import cl.uct.ojociudadano.ui.reports.IncidentLoadState
import cl.uct.ojociudadano.ui.reports.IncidentsUiState
import cl.uct.ojociudadano.ui.reports.incidentDate

@Composable
fun AdminIncidentDetailScreen(id: String?, onBack: () -> Unit) {
    val application = LocalContext.current.applicationContext as OjoCiudadanoApplication
    // The navigation entry owns this ViewModel. Restored route arguments recreate it
    // after process death; recompositions and configuration changes reuse it.
    val model: AdminIncidentDetailViewModel = viewModel(
        key = "admin-detail:$id",
        factory = remember(application, id) {
            AdminIncidentDetailViewModelFactory(application.container.incidentRepository, id)
        }
    )
    val state by model.state.collectAsState()
    AdminIncidentDetailContent(
        state = state,
        onBack = onBack,
        onRetry = model::retry,
        assignmentActions = AdminIncidentAssignmentActions(
            onOpen = model::openAssignmentPicker,
            onDismiss = model::dismissAssignmentPicker,
            onSelect = model::selectCandidate,
            onConfirm = model::confirmAssignment,
            onRetryCandidates = model::retryCandidates
        )
    )
}

@Composable
fun AdminIncidentDetailContent(
    state: AdminIncidentDetailState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    assignmentActions: AdminIncidentAssignmentActions = AdminIncidentAssignmentActions(),
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize()) {
        CitizenHeader("Detalle del incidente", subtitle = "Administrador", onBack = onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (val status = state.status) {
                IncidentsUiState.Loading -> IncidentLoadState(status, onRetry)
                is IncidentsUiState.Error -> {
                    CitizenMessage(status.message, isError = true)
                    if (state.canRetry) {
                        OutlinedButton(onClick = onRetry) { Text("Reintentar") }
                    }
                }
                IncidentsUiState.Success -> state.incident?.let { incident ->
                    AdminIncidentInformation(incident)
                    AdminIncidentAssignmentSection(
                        assignment = incident.activeAssignment,
                        state = state,
                        actions = assignmentActions
                    )
                }
                IncidentsUiState.Empty -> Unit
            }
        }
    }
}

@Composable
private fun AdminIncidentInformation(incident: Incident) {
    DetailSection("Identificación") {
        Text(
            incident.title.ifBlank { "Incidente" },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        DetailField("Categoría", incident.category.name)
        DetailField("Identificador", incident.id)
        AdminIncidentStatusBadge(incident.status.name)
    }
    DetailSection("Descripción") {
        Text(incident.description.ifBlank { "Descripción no disponible" }, style = MaterialTheme.typography.bodyLarge)
    }
    DetailSection("Ubicación") {
        val location = incident.location
        DetailField("Dirección", location.referenceAddress)
        if (location.sector.isNotBlank()) DetailField("Sector", location.sector)
        if (location.commune.isNotBlank()) DetailField("Comuna", location.commune)
        if (location.region.isNotBlank()) DetailField("Región", location.region)
        if (location.latitude.isFinite() && location.latitude in -90.0..90.0 &&
            location.longitude.isFinite() && location.longitude in -180.0..180.0
        ) {
            DetailField("Coordenadas", "${location.latitude}, ${location.longitude}")
        }
    }
    DetailSection("Fechas") {
        DetailField("Creación", incidentDate(incident.createdAt))
        if (incident.updatedAt.isNotBlank()) DetailField("Última actualización", incidentDate(incident.updatedAt))
    }
}

@Composable
internal fun DetailSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    OutlinedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                title,
                Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            content()
        }
    }
}

@Composable
internal fun DetailField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value.ifBlank { "No disponible" }, style = MaterialTheme.typography.bodyMedium)
    }
}
