package cl.uct.ojociudadano.ui.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.uct.ojociudadano.OjoCiudadanoApplication
import cl.uct.ojociudadano.ui.components.CitizenHeader

@Composable
fun IncidentDetailScreen(id: String, onBack: () -> Unit) {
    val application = LocalContext.current.applicationContext as OjoCiudadanoApplication
    val model: IncidentDetailViewModel = viewModel(key = "detail:$id", factory = remember(application, id) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                IncidentDetailViewModel(application.container.incidentRepository, id) as T
        }
    })
    val state by model.state.collectAsState()
    Column(Modifier.fillMaxSize()) {
        CitizenHeader("Detalle del incidente", onBack = onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            IncidentLoadState(state.status, model::load)
            state.incident?.let { incident ->
                IncidentPhoto(incident.id)
                Text(incident.category.name, color = MaterialTheme.colorScheme.primary)
                Text(incident.title, style = MaterialTheme.typography.headlineSmall)
                SuggestionChip(onClick = {}, label = { Text(incident.status.name) })
                Text(incident.description, style = MaterialTheme.typography.bodyLarge)
                HorizontalDivider()
                Text("Ubicación", style = MaterialTheme.typography.titleMedium)
                Text(listOfNotNull(incident.location.referenceAddress, incident.location.commune, incident.location.region)
                    .filter { it.isNotBlank() }.joinToString(" · "))
                Text("Reportado el "+incidentDate(incident.createdAt), style = MaterialTheme.typography.bodySmall)
                Text("Actualizado el "+incidentDate(incident.updatedAt), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
