package cl.uct.ojociudadano.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
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
    val unreadCount by model.unreadCount.collectAsState()
    val markingIds by model.markingIds.collectAsState()
    val isMarkingAll by model.isMarkingAll.collectAsState()
    val actionError by model.actionError.collectAsState()
    var invalidIncidentMessage by remember { mutableStateOf<String?>(null) }
    NotificationsContent(
        state = state,
        notifications = notifications,
        onBack = onBack,
        onRetry = model::retry,
        isRefreshing = isRefreshing,
        onRefresh = model::refresh,
        unreadCount = unreadCount,
        markingIds = markingIds,
        isMarkingAll = isMarkingAll,
        onMarkAsRead = model::markAsRead,
        onMarkAllAsRead = model::markAllAsRead,
        actionError = actionError,
        onDismissActionError = model::dismissActionError,
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
    onRefresh: () -> Unit = onRetry,
    unreadCount: Int = notifications.count { !it.isRead },
    markingIds: Set<String> = emptySet(),
    isMarkingAll: Boolean = false,
    onMarkAsRead: (Notification) -> Unit = {},
    onMarkAllAsRead: () -> Unit = {},
    actionError: String? = null,
    onDismissActionError: () -> Unit = {}
) {
    val showNotifications = when (state) {
        NotificationsUiState.Success -> true
        is NotificationsUiState.Error -> state.duringRefresh
        else -> false
    }
    Column(modifier.fillMaxSize()) {
        CitizenHeader(
            title = "Notificaciones",
            subtitle = "Ciudadano",
            onBack = onBack,
            actions = {
                MarkAllNotificationsAction(
                    enabled = unreadCount > 0 && !isMarkingAll,
                    isMarking = isMarkingAll,
                    onClick = onMarkAllAsRead
                )
            }
        )
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
                if (actionError != null) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CitizenMessage(
                                message = actionError,
                                modifier = Modifier.testTag(NotificationsTestTags.ACTION_ERROR),
                                isError = true
                            )
                            TextButton(onClick = onDismissActionError) { Text("Entendido") }
                        }
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
                            },
                            isMarking = notification.id in markingIds,
                            onMarkAsRead = if (notification.isRead) {
                                null
                            } else {
                                { onMarkAsRead(notification) }
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
    onClick: (() -> Unit)?,
    isMarking: Boolean = false,
    onMarkAsRead: (() -> Unit)? = null
) {
    val unread = !notification.isRead
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(NotificationsTestTags.card(notification.id))
            .semantics { stateDescription = if (unread) "No leída" else "Leída" }
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = if (unread) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            if (unread) {
                Box(
                    Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary)
                        .testTag(NotificationsTestTags.unreadIndicator(notification.id))
                        .semantics { contentDescription = "Notificación sin leer" }
                )
            }
            Column(
                Modifier.padding(Spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (unread) {
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
                    fontWeight = if (unread) FontWeight.SemiBold else FontWeight.Normal
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
                if (unread && onMarkAsRead != null) {
                    TextButton(
                        onClick = onMarkAsRead,
                        enabled = !isMarking,
                        modifier = Modifier.testTag(NotificationsTestTags.markRead(notification.id))
                    ) {
                        if (isMarking) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(14.dp)
                                    .testTag(NotificationsTestTags.marking(notification.id)),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Marcando…")
                        } else {
                            Text("Marcar como leída")
                        }
                    }
                }
            }
        }
    }
}

/** Acción de cabecera que marca todas las pendientes con una sola petición. */
@Composable
private fun MarkAllNotificationsAction(
    enabled: Boolean,
    isMarking: Boolean,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
        ),
        modifier = Modifier.testTag(NotificationsTestTags.MARK_ALL)
    ) {
        if (isMarking) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp).testTag(NotificationsTestTags.MARK_ALL_PROGRESS),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Icon(
                Icons.Default.DoneAll,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text("Marcar todas")
    }
}

internal object NotificationsTestTags {
    const val MARK_ALL = "notifications_mark_all"
    const val MARK_ALL_PROGRESS = "notifications_mark_all_progress"
    const val ACTION_ERROR = "notifications_action_error"

    fun card(id: String) = "notification_$id"
    fun markRead(id: String) = "notification_mark_read_$id"
    fun unreadIndicator(id: String) = "notification_unread_indicator_$id"
    fun marking(id: String) = "notification_marking_$id"
}
