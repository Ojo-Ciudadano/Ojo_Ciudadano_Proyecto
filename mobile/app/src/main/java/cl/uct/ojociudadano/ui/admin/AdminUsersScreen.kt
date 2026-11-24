package cl.uct.ojociudadano.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.uct.ojociudadano.OjoCiudadanoApplication
import cl.uct.ojociudadano.data.session.UserRole
import cl.uct.ojociudadano.ui.components.CitizenHeader
import cl.uct.ojociudadano.ui.components.LoadingContent

@Composable
fun AdminUsersScreen(onBack: () -> Unit) {
    val application = LocalContext.current.applicationContext as OjoCiudadanoApplication
    val model: AdminUsersViewModel = viewModel(factory = remember(application) {
        AdminUsersViewModelFactory(application.container.userRepository)
    })
    val state by model.state.collectAsStateWithLifecycle()
    AdminUsersContent(state, onBack, model::selectFilter, model::previousPage, model::nextPage, model::retry)
}

@Composable
fun AdminUsersContent(
    state: AdminUsersState,
    onBack: () -> Unit,
    onFilterSelected: (UserRoleFilter) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.page, state.filter) { listState.scrollToItem(0) }
    Column(modifier.fillMaxSize()) {
        CitizenHeader(title = "Usuarios", subtitle = "Administrador", onBack = onBack)
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text("Gestión de usuarios", style = MaterialTheme.typography.headlineSmall) }
            item { Text("Filtrar por rol", style = MaterialTheme.typography.titleMedium) }
            // Stacked controls also accommodate large fonts and narrow devices.
            item {
                Column {
                    UserRoleFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = state.filter == filter,
                            onClick = { onFilterSelected(filter) },
                            label = { Text(filter.label) }
                        )
                    }
                }
            }
            when (val status = state.status) {
                UsersLoadState.Loading -> item { LoadingContent(message = "Cargando usuarios…") }
                is UsersLoadState.Error -> item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(status.message)
                        TextButton(onClick = onRetry) { Text("Reintentar") }
                    }
                }
                UsersLoadState.Ready -> {
                    if (state.result?.items.isNullOrEmpty()) {
                        item { Text("No hay usuarios para el filtro y la página seleccionados.") }
                    }
                    items(state.result?.items.orEmpty(), key = { it.id }) { user ->
                        OutlinedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(user.name, style = MaterialTheme.typography.titleMedium)
                                Text(user.email, style = MaterialTheme.typography.bodyMedium)
                                val roles = user.roles.map { role ->
                                    when (UserRole.fromBackend(role)) {
                                        UserRole.ADMIN -> "Administrador"
                                        UserRole.CIUDADANO -> "Ciudadano"
                                        null -> "Rol no reconocido"
                                    }
                                }.ifEmpty { listOf("Sin rol asignado") }
                                Text("Rol: ${roles.joinToString(", ")}", color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
        if (state.status == UsersLoadState.Ready) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(if ((state.result?.totalPages ?: 0) == 0) "Sin páginas disponibles" else
                    "Página ${state.page} de ${state.result?.totalPages} · ${state.result?.total} usuarios")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onPrevious, enabled = state.canGoBack) { Text("Anterior") }
                    TextButton(onClick = onNext, enabled = state.canGoForward) { Text("Siguiente") }
                }
            }
        }
    }
}
