package cl.uct.ojociudadano.ui.notifications

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/** Texto del badge: vacío sin pendientes y "99+" a partir de 100. */
fun unreadBadgeLabel(count: Int): String = when {
    count <= 0 -> ""
    count > 99 -> "99+"
    else -> count.toString()
}

/**
 * Superpone el contador de no leídas al icono que recibe (#283). Sin
 * pendientes no dibuja nada: la navegación queda idéntica a la anterior.
 */
@Composable
fun NotificationBadge(
    count: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val label = unreadBadgeLabel(count)
    if (label.isEmpty()) {
        content()
        return
    }
    BadgedBox(
        badge = {
            Badge(
                modifier = Modifier
                    .testTag(NotificationsTestTags.BADGE)
                    .semantics { contentDescription = "Notificaciones sin leer: $count" }
            ) {
                Text(label)
            }
        },
        modifier = modifier
    ) {
        content()
    }
}
