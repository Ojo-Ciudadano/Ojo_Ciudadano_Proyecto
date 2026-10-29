package cl.uct.ojociudadano.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import android.net.Uri
import cl.uct.ojociudadano.ui.reports.IncidentDetailScreen
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import cl.uct.ojociudadano.ui.components.AppBottomNavigation
import cl.uct.ojociudadano.ui.home.HomeScreen
import cl.uct.ojociudadano.ui.login.LoginCredentials
import cl.uct.ojociudadano.ui.login.LoginScreen
import cl.uct.ojociudadano.ui.map.MapScreen
import cl.uct.ojociudadano.ui.notifications.NotificationsScreen
import cl.uct.ojociudadano.ui.profile.ProfileScreen
import cl.uct.ojociudadano.ui.report.CreateReportScreen
import cl.uct.ojociudadano.ui.reports.ReportsScreen

object AppRoutes {
    const val ADMIN = "admin"
    const val ROLE_UNAVAILABLE = "role_unavailable"
    const val HOME = "home"
    const val REPORTS = "reports"
    const val INCIDENT_DETAIL = "incident/{id}"
    const val CREATE_REPORT = "create_report"
    const val NOTIFICATIONS = "notifications"
    const val MAP = "map"
    const val PROFILE = "profile"
    const val REGISTER = "register"
    const val LOGIN = "login"
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = AppRoutes.HOME,
    onLogin: (LoginCredentials) -> Unit = {},
    onLogout: () -> Unit = {},
    loginContent: (@Composable () -> Unit)? = null,
    registrationContent: (@Composable (onBack: () -> Unit) -> Unit)? = null
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    fun navigateToHome() {
        if (!navController.popBackStack(AppRoutes.HOME, inclusive = false)) {
            navController.navigate(AppRoutes.HOME)
        }
    }

    Scaffold(
        bottomBar = {
            if (currentRoute in citizenRoutes) {
                AppBottomNavigation(currentRoute = currentRoute) { route ->
                    if (route == AppRoutes.HOME) {
                        navigateToHome()
                    } else {
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            enterTransition = { fadeIn(tween(180)) },
            exitTransition = { fadeOut(tween(120)) },
            popEnterTransition = { fadeIn(tween(180)) },
            popExitTransition = { fadeOut(tween(120)) },
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppRoutes.ADMIN) {
                SessionDestinationScreen(
                    title = "Administrador",
                    message = "Pantalla provisional. Las funcionalidades administrativas aún no están disponibles.",
                    onLogout = onLogout
                )
            }
            composable(AppRoutes.ROLE_UNAVAILABLE) {
                SessionDestinationScreen(
                    title = "No se puede determinar tu acceso",
                    message = "La sesión no tiene un único rol válido. Cierra sesión e inicia nuevamente. Si el problema continúa, contacta al administrador.",
                    onLogout = onLogout
                )
            }
            composable(AppRoutes.LOGIN) {
                loginContent?.invoke() ?: LoginScreen(
                    onLogin = onLogin,
                    onRegister = registrationContent?.let { { navController.navigate(AppRoutes.REGISTER) } }
                )
            }
            composable(AppRoutes.REGISTER) {
                registrationContent?.invoke { navController.popBackStack(AppRoutes.LOGIN, false) }
            }
            composable(AppRoutes.HOME) {
                HomeScreen(
                    onReportsClick = { navController.navigate(AppRoutes.REPORTS) },
                    onCreateReportClick = { navController.navigate(AppRoutes.CREATE_REPORT) },
                    onNotificationsClick = { navController.navigate(AppRoutes.NOTIFICATIONS) },
                    onMapClick = { navController.navigate(AppRoutes.MAP) },
                    onIncidentClick = { navController.navigate("incident/${Uri.encode(it)}") }
                )
            }
            composable(AppRoutes.REPORTS) {
                ReportsScreen(
                    onBack = ::navigateToHome,
                    onCreateReportClick = { navController.navigate(AppRoutes.CREATE_REPORT) },
                    onIncidentClick = { navController.navigate("incident/${Uri.encode(it)}") }
                )
            }
            composable(AppRoutes.INCIDENT_DETAIL) { entry ->
                IncidentDetailScreen(entry.arguments?.getString("id").orEmpty(), onBack = { navController.navigateUp() })
            }
            composable(AppRoutes.CREATE_REPORT) {
                CreateReportScreen(onBack = ::navigateToHome)
            }
            composable(AppRoutes.NOTIFICATIONS) {
                NotificationsScreen(onBack = navController::navigateUp)
            }
            composable(AppRoutes.MAP) { MapScreen() }
            composable(AppRoutes.PROFILE) { ProfileScreen(onLogout = onLogout) }
        }
    }
}

private val citizenRoutes = setOf(
    AppRoutes.HOME, AppRoutes.REPORTS, AppRoutes.CREATE_REPORT,
    AppRoutes.NOTIFICATIONS, AppRoutes.MAP, AppRoutes.PROFILE, AppRoutes.INCIDENT_DETAIL
)
