package cl.uct.ojociudadano.ui.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.uct.ojociudadano.OjoCiudadanoApplication
import cl.uct.ojociudadano.data.model.Category
import cl.uct.ojociudadano.ui.components.CitizenHeader
import cl.uct.ojociudadano.ui.components.CitizenMessage

@Composable
fun ReportsScreen(onBack: () -> Unit = {}, onCreateReportClick: () -> Unit = {},
                  onIncidentClick: (String) -> Unit = {}) {
    val application = LocalContext.current.applicationContext as OjoCiudadanoApplication
    val model: ReportsViewModel = viewModel(factory = remember(application) {
        ReportsViewModelFactory(application.container.incidentRepository, application.container.categoryRepository)
    })
    val incidents by model.incidents.collectAsState()
    val categories by model.categories.collectAsState()
    val state by model.uiState.collectAsState()
    val scope by model.scope.collectAsState()
    var categoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var date by rememberSaveable { mutableStateOf(DateFilter.RECENT) }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var categoryMenu by remember { mutableStateOf(false) }
    var dateMenu by remember { mutableStateOf(false) }
    val category = categories.firstOrNull { it.id == categoryId }
    Column(Modifier.fillMaxSize()) {
        CitizenHeader("Incidentes", onBack = onBack)
        TabRow(selectedTabIndex = if (scope == IncidentScope.ALL) 0 else 1) {
            Tab(scope == IncidentScope.ALL, onClick = { model.selectScope(IncidentScope.ALL) }, text = { Text("Todos los incidentes") })
            Tab(scope == IncidentScope.MINE, onClick = { model.selectScope(IncidentScope.MINE) }, text = { Text("Mis incidentes") })
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { showFilters = !showFilters }) { Text(if (showFilters) "Ocultar filtros" else "Filtrar") }
            TextButton(onClick = onCreateReportClick) { Text("+ Nuevo reporte") }
        }
        if (showFilters) {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) {
                    OutlinedButton(onClick = { categoryMenu = true }) { Text(category?.name ?: "Categorías") }
                    DropdownMenu(categoryMenu, { categoryMenu = false }) {
                        (listOf<Category?>(null) + categories).forEach { item ->
                            DropdownMenuItem(text = { Text(item?.name ?: "Todas las categorías") }, onClick = {
                                categoryId = item?.id; categoryMenu = false; model.filterIncidents(item, date)
                            })
                        }
                    }
                }
                Box(Modifier.weight(1f)) {
                    OutlinedButton(onClick = { dateMenu = true }) { Text(dateLabel(date)) }
                    DropdownMenu(dateMenu, { dateMenu = false }) {
                        DateFilter.entries.forEach { item ->
                            DropdownMenuItem(text = { Text(dateLabel(item)) }, onClick = {
                                date = item; dateMenu = false; model.filterIncidents(category, item)
                            })
                        }
                    }
                }
            }
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Text(if (scope == IncidentScope.MINE) "Reportes de tu cuenta" else "Reportes disponibles para tu sesión",
                style = MaterialTheme.typography.bodyMedium) }
            if (state != IncidentsUiState.Success) {
                item { IncidentLoadState(state, model::loadIncidents) }
            } else if (incidents.isEmpty()) {
                item { CitizenMessage("No hay resultados para estos filtros.") }
            } else items(incidents, key = { it.id }) { incident ->
                IncidentCard(incident) { onIncidentClick(incident.id) }
            }
        }
    }
}

private fun dateLabel(filter: DateFilter): String = when (filter) {
    DateFilter.RECENT -> "Más recientes"
    DateFilter.TODAY -> "Hoy"
    DateFilter.WEEK -> "Última semana"
    DateFilter.MONTH -> "Último mes"
    DateFilter.YEAR -> "Último año"
}
