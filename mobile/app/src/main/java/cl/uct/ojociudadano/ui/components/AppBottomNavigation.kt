package cl.uct.ojociudadano.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import cl.uct.ojociudadano.navigation.AppRoutes
import cl.uct.ojociudadano.ui.notifications.NotificationBadge

private data class NavigationDestination(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val showsUnreadBadge: Boolean = false
)

private val navigationDestinations = listOf(
    NavigationDestination(AppRoutes.HOME, "Inicio", Icons.Filled.Home),
    NavigationDestination(AppRoutes.CREATE_REPORT, "Reportar", Icons.Filled.AddCircle),
    NavigationDestination(AppRoutes.MAP, "Mapa", Icons.Filled.Map),
    NavigationDestination(
        AppRoutes.NOTIFICATIONS,
        "Avisos",
        Icons.Filled.Notifications,
        showsUnreadBadge = true
    ),
    NavigationDestination(AppRoutes.PROFILE, "Perfil", Icons.Filled.Person)
)

@Composable
fun AppBottomNavigation(
    currentRoute: String?,
    isDestinationAllowed: (String) -> Boolean,
    unreadCount: Int = 0,
    onDestinationClick: (String) -> Unit
) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        navigationDestinations.filter { isDestinationAllowed(it.route) }.forEach { destination ->
            val selected = currentRoute == destination.route ||
                (destination.route == AppRoutes.HOME && currentRoute in setOf(AppRoutes.REPORTS, AppRoutes.INCIDENT_DETAIL))

            NavigationBarItem(
                selected = selected,
                onClick = { onDestinationClick(destination.route) },
                icon = {
                    if (destination.showsUnreadBadge) {
                        NotificationBadge(count = unreadCount) {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label
                            )
                        }
                    } else {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = destination.label
                        )
                    }
                },
                label = { Text(destination.label) },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
