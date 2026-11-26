package cl.uct.ojociudadano.ui.notifications

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
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
import cl.uct.ojociudadano.navigation.NotificationClickOutcome
import cl.uct.ojociudadano.navigation.notificationClickOutcome
import cl.uct.ojociudadano.ui.components.CitizenHeader
import cl.uct.ojociudadano.ui.components.CitizenMessage
import cl.uct.ojociudadano.ui.reports.incidentDate
import cl.uct.ojociudadano.ui.theme.Spacing

@Composable
fun NotificationsScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onOpenIncident: (String) -> Unit = {}
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
    val isRefreshing by model.isRefreshing.collectAsState()
    var invalidIncidentMessage by remember { mutableStateOf<String?>(null) }
    NotificationsContent(
        state = state,
        notifications = notifications,
        onBack = onBack,
        onRetry = model::retry,
        isRefreshing = isRefreshing,
        onRefresh = model::refresh,
        onNotificationClick = { notification ->
            // El marcado es best-effort; la navegación no espera al backend.
            model.openNotification(notification)
            onOpenIncident(notification.incidentId)
        },
        onInvalidIncident = {
            invalidIncidentMessage = "El incidente asociado a esta notificación no es válido. " +
                "Intenta nuevamente más tarde."
        },
        invalidIncidentMessage = invalidIncidentMessage,
        onDismissInvalidIncident = { invalidIncidentMessage = null },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsContent(
    state: NotificationsUiState,
    notifications: List<Notification>,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onNotificationClick: (Notification) -> Unit = {},
    onInvalidIncident: (Notification) -> Unit = {},
    invalidIncidentMessage: String? = null,
    onDismissInvalidIncident: () -> Unit = {},
    modifier: Modifier = Modifier,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = onRetry
) {
    val showNotifications = when (state) {
        NotificationsUiState.Success -> true
        is NotificationsUiState.Error -> state.duringRefresh
        else -> false
    }
    Column(modifier.fillMaxSize()) {
        CitizenHeader(title = "Notificaciones", subtitle = "Ciudadano", onBack = onBack)
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.weight(1f).fillMaxWidth().testTag("notifications_refresh")
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(Spacing.medium)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
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
                if (invalidIncidentMessage != null) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CitizenMessage(invalidIncidentMessage, isError = true)
                            TextButton(onClick = onDismissInvalidIncident) { Text("Entendido") }
                        }
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
                    NotificationsUiState.Success -> Unit
                }
                if (showNotifications) {
                    items(notifications, key = { it.id }) { notification ->
                        val outcome = notificationClickOutcome(notification.incidentId)
                        NotificationCard(
                            notification = notification,
                            onClick = when (outcome) {
                                is NotificationClickOutcome.Open -> {
                                    { onNotificationClick(notification) }
                                }
                                NotificationClickOutcome.InvalidIncident -> {
                                    // No se marca como leída ni se navega a una ruta inválida.
                                    { onInvalidIncident(notification) }
                                }
                                NotificationClickOutcome.NoIncident -> null
                            }
                        )
                    }
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
private fun NotificationCard(
    notification: Notification,
    onClick: (() -> Unit)?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("notification_${notification.id}")
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(Spacing.medium), verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!notification.isRead) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
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
            // La referencia se conserva solo si el backend la entregó; la tarjeta
            // solo es clicable en ese caso.
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
