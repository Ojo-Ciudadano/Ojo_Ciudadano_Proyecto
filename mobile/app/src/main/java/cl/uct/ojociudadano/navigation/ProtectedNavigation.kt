package cl.uct.ojociudadano.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navOptions
import cl.uct.ojociudadano.data.session.SessionManager

/** Check the current session at click time, even if the callback predates a session change. */
fun NavHostController.navigateIfAllowed(
    sessionManager: SessionManager,
    route: String,
    options: NavOptions = navOptions { launchSingleTop = true }
): Boolean {
    if (!RoleAccessPolicy.allows(sessionManager.state.value, route)) return false
    navigate(route, options)
    return true
}

/** Protect content even when a caller bypasses navigateIfAllowed or restores an old entry. */
internal fun NavGraphBuilder.protectedComposable(
    sessionManager: SessionManager,
    navController: NavHostController,
    route: String,
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) {
    composable(route) { entry ->
        val state by sessionManager.state.collectAsState()
        if (RoleAccessPolicy.allows(state, route)) {
            content(entry)
        } else {
            // Never compose restricted screens, their ViewModels or their actions, even for a frame.
            LaunchedEffect(state, entry.id) {
                if (navController.currentBackStackEntry != entry) return@LaunchedEffect
                val currentState = sessionManager.state.value
                if (RoleAccessPolicy.allows(currentState, route)) return@LaunchedEffect
                val safeRoute = RoleAccessPolicy.safeRoute(currentState) ?: return@LaunchedEffect
                RoleAccessPolicy.registeredRoutes.forEach { navController.clearBackStack(it) }
                navController.navigate(safeRoute) {
                    // Discard all active and saved entries owned by this graph. Back cannot
                    // resurrect the rejected destination or an earlier restricted screen.
                    popUpTo(navController.graph.id) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }
}
