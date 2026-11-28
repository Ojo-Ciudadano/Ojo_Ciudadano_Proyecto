package cl.uct.ojociudadano.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.uct.ojociudadano.OjoCiudadanoApplication
import cl.uct.ojociudadano.data.session.SessionManager
import cl.uct.ojociudadano.data.session.UserRole
import cl.uct.ojociudadano.ui.components.CitizenHeader
import cl.uct.ojociudadano.ui.components.LoadingContent

@Composable
fun AdminUsersScreen(sessionManager: SessionManager, onBack: () -> Unit) {
    val application = LocalContext.current.applicationContext as OjoCiudadanoApplication
    val model: AdminUsersViewModel = viewModel(factory = remember(application, sessionManager) {
        AdminUsersViewModelFactory(application.container.userRepository, sessionManager)
    })
    val state by model.state.collectAsStateWithLifecycle()
    AdminUsersContent(
        state = state,
        onBack = onBack,
        onFilterSelected = model::selectFilter,
        onPrevious = model::previousPage,
        onNext = model::nextPage,
        onRetry = model::retry,
        onRequestRoleChange = model::requestRoleChange,
        onDismissRoleChange = model::dismissRoleChange,
        onConfirmRoleChange = model::confirmRoleChange
    )
}

@Composable
fun AdminUsersContent(
    state: AdminUsersState,
    onBack: () -> Unit,
    onFilterSelected: (UserRoleFilter) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRetry: () -> Unit,
    onRequestRoleChange: (String) -> Unit,
    onDismissRoleChange: () -> Unit,
    onConfirmRoleChange: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.page, state.filter) { listState.scrollToItem(0) }
    Column(modifier.fillMaxSize()) {
        CitizenHeader(title = "Usuarios", subtitle = "Administrador", onBack = onBack)
        state.roleChangeError?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite }
            )
        }
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
                UsersLoadState.Loading -> item {
                    if (state.result == null) LoadingContent(message = "Cargando usuarios…")
                    else Text("Actualizando usuarios…")
                }
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
                }
            }
            items(state.result?.items.orEmpty(), key = { it.id }) { user ->
                val isCurrentUser = state.isCurrentUser(user.id)
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(user.name, style = MaterialTheme.typography.titleMedium)
                        if (isCurrentUser) {
                            Text(
                                "Tú",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        Text(user.email, style = MaterialTheme.typography.bodyMedium)
                        val roles = user.roles.map { role ->
                            UserRole.fromBackend(role)?.label ?: "Rol no reconocido"
                        }.ifEmpty { listOf("Sin rol asignado") }
                        Text(
                            "Rol: ${roles.joinToString(", ")}",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge
                        )
                        if (!isCurrentUser && user.id in state.changingRoleIds) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                Text("Cambiando rol…")
                            }
                        } else {
                            TextButton(
                                onClick = { onRequestRoleChange(user.id) },
                                enabled = state.canChangeRole(user.id) && user.alternativeRole() != null
                            ) { Text("Cambiar rol") }
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
    state.roleChange?.let { change ->
        AlertDialog(
            onDismissRequest = onDismissRoleChange,
            title = { Text("Cambiar rol") },
            text = { Text("¿Quieres cambiar el rol de ${change.userName} a ${change.newRole.label}?") },
            dismissButton = { TextButton(onClick = onDismissRoleChange) { Text("Cancelar") } },
            confirmButton = { TextButton(onClick = onConfirmRoleChange) { Text("Confirmar") } }
        )
    }
}

private val UserRole.label: String
    get() = when (this) {
        UserRole.ADMIN -> "Administrador"
        UserRole.CIUDADANO -> "Ciudadano"
    }
