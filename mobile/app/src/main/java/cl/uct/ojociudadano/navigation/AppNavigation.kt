package cl.uct.ojociudadano.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import android.net.Uri
import cl.uct.ojociudadano.ui.reports.IncidentDetailScreen
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.navOptions
import cl.uct.ojociudadano.data.session.SessionManager
import cl.uct.ojociudadano.data.session.SessionState
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import cl.uct.ojociudadano.ui.components.AppBottomNavigation
import cl.uct.ojociudadano.ui.admin.AdminHomeScreen
import cl.uct.ojociudadano.ui.admin.AdminIncidentsPendingScreen
import cl.uct.ojociudadano.ui.home.HomeScreen
import cl.uct.ojociudadano.ui.login.LoginCredentials
import cl.uct.ojociudadano.ui.login.LoginScreen
import cl.uct.ojociudadano.ui.map.MapScreen
import cl.uct.ojociudadano.ui.notifications.NotificationsScreen
import cl.uct.ojociudadano.ui.profile.ProfileScreen
import cl.uct.ojociudadano.ui.report.CreateReportExitPolicy
import cl.uct.ojociudadano.ui.report.CreateReportExitRisk
import cl.uct.ojociudadano.ui.report.CreateReportScreen
import cl.uct.ojociudadano.ui.reports.ReportsScreen

object AppRoutes {
    const val ADMIN = "admin"
    const val ADMIN_INCIDENTS = "admin/incidents"
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
    sessionManager: SessionManager,
    navController: NavHostController = rememberNavController(),
    startDestination: String = AppRoutes.HOME,
    onLogin: (LoginCredentials) -> Unit = {},
    onLogout: () -> Unit = {},
    loginContent: (@Composable () -> Unit)? = null,
    registrationContent: (@Composable (onBack: () -> Unit) -> Unit)? = null
) {
    val sessionState by sessionManager.state.collectAsState()
    fun navigate(route: String) {
        navController.navigateIfAllowed(sessionManager, route)
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    fun navigateToHome() {
        if (!RoleAccessPolicy.allows(sessionManager.state.value, AppRoutes.HOME)) return
        if (!navController.popBackStack(AppRoutes.HOME, inclusive = false)) {
            navigate(AppRoutes.HOME)
        }
    }

    var createReportExitRisk by remember {
        mutableStateOf<CreateReportExitRisk?>(null)
    }
    var pendingExitAction by remember {
        mutableStateOf<(() -> Unit)?>(null)
    }
    var showExitConfirmation by remember { mutableStateOf(false) }

    fun requestNavigation(action: () -> Unit) {
        if (createReportExitRisk == null) {
            action()
        } else {
            pendingExitAction = action
            showExitConfirmation = true
        }
    }

    LaunchedEffect(createReportExitRisk) {
        if (createReportExitRisk == null) {
            showExitConfirmation = false
            pendingExitAction = null
        }
    }

    Scaffold(
        bottomBar = {
            if (currentRoute in citizenRoutes && RoleAccessPolicy.allows(sessionState, currentRoute)) {
                AppBottomNavigation(
                    currentRoute = currentRoute,
                    isDestinationAllowed = { RoleAccessPolicy.allows(sessionState, it) }
                ) { route ->
                    if (route != currentRoute) {
                        requestNavigation {
                            if (route == AppRoutes.HOME) {
                                navigateToHome()
                            } else {
                                navController.navigateIfAllowed(
                                    sessionManager,
                                    route,
                                    navOptions {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                )
                            }
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
            protectedComposable(sessionManager, navController, AppRoutes.ADMIN) {
                val session = (sessionState as? SessionState.Authenticated)?.session
                AdminHomeScreen(
                    name = session?.name.orEmpty(),
                    email = session?.email.orEmpty(),
                    onIncidentManagementClick = { navigate(AppRoutes.ADMIN_INCIDENTS) },
                    onLogout = onLogout
                )
            }
            protectedComposable(sessionManager, navController, AppRoutes.ADMIN_INCIDENTS) {
                AdminIncidentsPendingScreen(onBack = {
                    if (RoleAccessPolicy.allows(sessionManager.state.value, AppRoutes.ADMIN) &&
                        !navController.popBackStack(AppRoutes.ADMIN, inclusive = false)
                    ) {
                        navigate(AppRoutes.ADMIN)
                    }
                })
            }
            protectedComposable(sessionManager, navController, AppRoutes.ROLE_UNAVAILABLE) {
                SessionDestinationScreen(
                    title = "No se puede determinar tu acceso",
                    message = "La sesión no tiene un único rol válido. Cierra sesión e inicia nuevamente. Si el problema continúa, contacta al administrador.",
                    onLogout = onLogout
                )
            }
            protectedComposable(sessionManager, navController, AppRoutes.LOGIN) {
                loginContent?.invoke() ?: LoginScreen(
                    onLogin = onLogin,
                    onRegister = registrationContent?.let { { navigate(AppRoutes.REGISTER) } }
                )
            }
            protectedComposable(sessionManager, navController, AppRoutes.REGISTER) {
                registrationContent?.invoke { navController.popBackStack(AppRoutes.LOGIN, false) }
            }
            protectedComposable(sessionManager, navController, AppRoutes.HOME) {
                HomeScreen(
                    onReportsClick = { navigate(AppRoutes.REPORTS) },
                    onCreateReportClick = { navigate(AppRoutes.CREATE_REPORT) },
                    onNotificationsClick = { navigate(AppRoutes.NOTIFICATIONS) },
                    onMapClick = { navigate(AppRoutes.MAP) },
                    onIncidentClick = { navigate("incident/${Uri.encode(it)}") }
                )
            }
            protectedComposable(sessionManager, navController, AppRoutes.REPORTS) {
                ReportsScreen(
                    onBack = ::navigateToHome,
                    onCreateReportClick = { navigate(AppRoutes.CREATE_REPORT) },
                    onIncidentClick = { navigate("incident/${Uri.encode(it)}") }
                )
            }
            protectedComposable(sessionManager, navController, AppRoutes.INCIDENT_DETAIL) { entry ->
                IncidentDetailScreen(entry.arguments?.getString("id").orEmpty(), onBack = { navController.navigateUp() })
            }
            protectedComposable(sessionManager, navController, AppRoutes.CREATE_REPORT) {
                CreateReportScreen(
                    onBack = { requestNavigation { navigateToHome() } },
                    onExitRiskChanged = { createReportExitRisk = it }
                )
            }
            protectedComposable(sessionManager, navController, AppRoutes.NOTIFICATIONS) {
                NotificationsScreen(onBack = navController::navigateUp)
            }
            protectedComposable(sessionManager, navController, AppRoutes.MAP) { MapScreen() }
            protectedComposable(sessionManager, navController, AppRoutes.PROFILE) { ProfileScreen(onLogout = onLogout) }
        }
    }

    val exitRisk = createReportExitRisk
    if (showExitConfirmation && exitRisk != null) {
        AlertDialog(
            onDismissRequest = {
                showExitConfirmation = false
                pendingExitAction = null
            },
            title = { Text("Salir del reporte") },
            text = { Text(CreateReportExitPolicy.message(exitRisk)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val action = pendingExitAction
                        showExitConfirmation = false
                        pendingExitAction = null
                        action?.invoke()
                    }
                ) {
                    Text("Salir y descartar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showExitConfirmation = false
                        pendingExitAction = null
                    }
                ) {
                    Text("Seguir aquí")
                }
            }
        )
    }
}

private val citizenRoutes = setOf(
    AppRoutes.HOME, AppRoutes.REPORTS, AppRoutes.CREATE_REPORT,
    AppRoutes.NOTIFICATIONS, AppRoutes.MAP, AppRoutes.PROFILE, AppRoutes.INCIDENT_DETAIL
)
