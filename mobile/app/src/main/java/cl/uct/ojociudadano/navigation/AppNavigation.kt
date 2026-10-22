package cl.uct.ojociudadano.navigation

import androidx.compose.runtime.Composable
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
    const val HOME = "home"
    const val REPORTS = "reports"
    const val CREATE_REPORT = "create_report"
    const val NOTIFICATIONS = "notifications"
    const val MAP = "map"
    const val PROFILE = "profile"
    const val LOGIN = "login"
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = AppRoutes.HOME,
    onLogin: (LoginCredentials) -> Unit = {},
    onLogout: () -> Unit = {},
    loginContent: (@Composable () -> Unit)? = null
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
            if (currentRoute != AppRoutes.LOGIN) {
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
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppRoutes.LOGIN) {
                loginContent?.invoke() ?: LoginScreen(onLogin = onLogin)
            }
            composable(AppRoutes.HOME) {
                HomeScreen(
                    onReportsClick = { navController.navigate(AppRoutes.REPORTS) },
                    onCreateReportClick = { navController.navigate(AppRoutes.CREATE_REPORT) },
                    onNotificationsClick = { navController.navigate(AppRoutes.NOTIFICATIONS) }
                )
            }
            composable(AppRoutes.REPORTS) {
                ReportsScreen(
                    onBack = ::navigateToHome,
                    onCreateReportClick = { navController.navigate(AppRoutes.CREATE_REPORT) }
                )
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
