package cl.uct.ojociudadano.ui.notifications

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.uct.ojociudadano.OjoCiudadanoApplication
import cl.uct.ojociudadano.data.model.Notification
import cl.uct.ojociudadano.ui.components.CitizenHeader
import cl.uct.ojociudadano.ui.components.CitizenMessage
import cl.uct.ojociudadano.ui.reports.incidentDate

@Composable
fun NotificationsScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val application = LocalContext.current.applicationContext as OjoCiudadanoApplication
    val model: NotificationsViewModel = viewModel(factory = remember(application) {
        NotificationsViewModel.Factory(application.container.notificationRepository)
    })
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, model) {
        val observer = LifecycleEventObserver { _, event ->
            // Al volver a la pantalla se refresca contra la capa de datos; el
            // ViewModel evita duplicar la consulta si todavía está en curso.
            if (event == Lifecycle.Event.ON_RESUME) model.load()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val state by model.state.collectAsState()
    val notifications by model.notifications.collectAsState()
    NotificationsContent(
        state = state,
        notifications = notifications,
        onBack = onBack,
        onRetry = model::retry,
        modifier = modifier
    )
}

@Composable
fun NotificationsContent(
    state: NotificationsUiState,
    notifications: List<Notification>,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize()) {
        CitizenHeader(title = "Notificaciones", subtitle = "Ciudadano", onBack = onBack)
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Tus notificaciones",
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Novedades de los reportes asociados a tu cuenta.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            when (state) {
                NotificationsUiState.Loading -> item { NotificationsLoading() }
                NotificationsUiState.Empty -> item {
                    CitizenMessage("Aún no tienes notificaciones. Te avisaremos cuando haya novedades de tus reportes.")
                }
                is NotificationsUiState.Error -> item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CitizenMessage(state.message, isError = true)
                        if (state.canRetry) {
                            OutlinedButton(onClick = onRetry) { Text("Reintentar") }
                        }
                    }
                }
                NotificationsUiState.Success -> items(notifications, key = { it.id }) { notification ->
                    NotificationCard(notification)
                }
            }
        }
    }
}

@Composable
private fun NotificationsLoading() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator(Modifier.size(28.dp))
        Text("Cargando notificaciones…")
    }
}

@Composable
private fun NotificationCard(notification: Notification) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!notification.isRead) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            "Nueva",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Text(
                    incidentDate(notification.createdAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                notification.message,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            // La referencia se conserva solo si el backend la entregó; navegar al
            // incidente queda fuera del alcance de esta issue.
            if (notification.incidentId.isNotBlank()) {
                Text(
                    "Incidente: ${notification.incidentId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
