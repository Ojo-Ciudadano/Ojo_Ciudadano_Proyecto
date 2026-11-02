package cl.uct.ojociudadano.ui.startup

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import cl.uct.ojociudadano.data.repository.AuthRepository
import cl.uct.ojociudadano.data.session.AuthenticatedSession
import cl.uct.ojociudadano.data.session.DefaultSessionManager
import cl.uct.ojociudadano.data.session.SessionManager
import cl.uct.ojociudadano.data.session.SessionStorage
import cl.uct.ojociudadano.data.session.SessionState
import cl.uct.ojociudadano.navigation.AppNavigation
import cl.uct.ojociudadano.navigation.navigateIfAllowed
import cl.uct.ojociudadano.navigation.AppRoutes
import cl.uct.ojociudadano.ui.theme.OjoCiudadanoTheme
import cl.uct.ojociudadano.ui.login.LoginTestTags
import java.util.Base64
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import cl.uct.ojociudadano.data.model.RegisteredUser
import cl.uct.ojociudadano.data.model.RegistrationInput

class RoleNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun citizenRestorationAccountChangeAndLogoutResetNavigation() {
        val manager = DefaultSessionManager(Storage(session("CIUDADANO")))
        showGate(manager)
        compose.waitUntil(5_000) { manager.state.value is SessionState.Authenticated }
        compose.onNodeWithTag("citizen_home").assertIsDisplayed()
        compose.onNodeWithText("Administrador").assertDoesNotExist()
        compose.onNodeWithText("Perfil").performClick()
        compose.runOnIdle { assertTrue(compose.activity.onBackPressedDispatcher.hasEnabledCallbacks()) }
        runBlocking { manager.establish(session("CIUDADANO").copy(userId = "another-citizen")) }
        compose.onNodeWithTag("citizen_home").assertIsDisplayed()
        compose.onNodeWithText("Perfil").performClick()
        compose.onNodeWithText("Cerrar sesión").performClick()
        compose.waitUntil(5_000) { manager.state.value == SessionState.Unauthenticated }
        compose.onNodeWithTag(LoginTestTags.EMAIL_FIELD).assertIsDisplayed()
        compose.onNodeWithTag("citizen_home").assertDoesNotExist()
        compose.runOnIdle { assertFalse(compose.activity.onBackPressedDispatcher.hasEnabledCallbacks()) }
    }

    @Test fun successfulLoginShowsAdministratorAndLogoutReturnsToFreshLogin() {
        val manager = DefaultSessionManager(Storage(null))
        compose.setContent {
            OjoCiudadanoTheme {
                SessionStartupGate(manager, object : AuthRepository {
                    override suspend fun register(input: RegistrationInput): RegisteredUser =
                        error("Registration is not used by this test")

                    override suspend fun login(email: String, password: String) = session("ADMIN")
                })
            }
        }
        compose.waitUntil(5_000) { manager.state.value == SessionState.Unauthenticated }
        compose.onNodeWithTag(LoginTestTags.EMAIL_FIELD).performTextInput("test@example.invalid")
        compose.onNodeWithTag(LoginTestTags.PASSWORD_FIELD).performTextInput("Password123!")
        compose.onNodeWithTag(LoginTestTags.SUBMIT_BUTTON).performClick()
        compose.waitUntil(5_000) { manager.state.value is SessionState.Authenticated }
        compose.onNodeWithText("Administrador").assertIsDisplayed()
        compose.onNodeWithText("Cerrar sesión").performClick()
        compose.waitUntil(5_000) { manager.state.value == SessionState.Unauthenticated }
        compose.onNodeWithTag(LoginTestTags.EMAIL_FIELD).assertIsDisplayed()
        compose.onNodeWithText("Administrador").assertDoesNotExist()
    }

    @Test fun restorationWaitsBeforeShowingAdministratorAndLogoutRemovesIt() {
        val manager = DefaultSessionManager(Storage(session("ADMIN")))
        val ready = CompletableDeferred<Unit>()
        val delayed = object : SessionManager by manager {
            override suspend fun restore() = run { ready.await(); manager.restore() }
        }
        showGate(delayed)
        compose.onNodeWithText("Conectando...").assertIsDisplayed()
        compose.onNodeWithText("Administrador").assertDoesNotExist()
        compose.onNodeWithTag("citizen_home").assertDoesNotExist()
        ready.complete(Unit)
        compose.waitForIdle()
        compose.waitUntil(5_000) { manager.state.value is SessionState.Authenticated }
        compose.onNodeWithText("Administrador").assertIsDisplayed()
        compose.onNodeWithText("Cerrar sesión").performClick()
        compose.waitUntil(5_000) { manager.state.value == SessionState.Unauthenticated }
        compose.onNodeWithText("Administrador").assertDoesNotExist()
    }

    @Test fun changingRoleForSameAccountDisposesPreviousNavigation() {
        val manager = DefaultSessionManager(Storage(session("ADMIN")))
        showGate(manager)
        compose.waitUntil(5_000) { manager.state.value is SessionState.Authenticated }
        compose.onNodeWithText("Administrador").assertIsDisplayed()
        runBlocking { manager.establish(session()) }
        compose.onNodeWithText("No se puede determinar tu acceso").assertIsDisplayed()
        compose.onNodeWithText("Administrador").assertDoesNotExist()
        runBlocking { manager.establish(session("ADMIN").copy(userId = "another-user")) }
        compose.onNodeWithText("Administrador").assertIsDisplayed()
        compose.onNodeWithText("No se puede determinar tu acceso").assertDoesNotExist()
    }

    @Test fun recompositionPreservesCurrentDestinationWithoutAddingEntries() {
        val manager = DefaultSessionManager(Storage(null))
        runBlocking { manager.restore() }
        val revision = mutableStateOf(0)
        lateinit var controller: NavHostController
        var changes = 0
        compose.setContent {
            revision.value
            controller = rememberNavController()
            OjoCiudadanoTheme {
                AppNavigation(manager, controller, startDestination = AppRoutes.LOGIN,
                    loginContent = {}, registrationContent = {})
            }
        }
        compose.runOnIdle {
            controller.addOnDestinationChangedListener { _, _, _ -> changes++ }
            controller.navigate(AppRoutes.REGISTER)
        }
        compose.runOnIdle { revision.value++ }
        compose.runOnIdle {
            assertEquals(2, changes)
            assertEquals(AppRoutes.REGISTER, controller.currentDestination?.route)
            assertTrue(controller.popBackStack())
            assertFalse(controller.popBackStack())
        }
    }

    @Test fun administratorCannotNavigateToCitizenDestinationsEvenBypassingActionGuard() {
        val manager = DefaultSessionManager(Storage(session("ADMIN")))
        runBlocking { manager.restore() }
        lateinit var controller: NavHostController
        compose.setContent {
            controller = rememberNavController()
            OjoCiudadanoTheme {
                AppNavigation(manager, controller, startDestination = AppRoutes.ADMIN)
            }
        }
        compose.onNodeWithText("Administrador").assertIsDisplayed()
        for (route in listOf(AppRoutes.HOME, AppRoutes.CREATE_REPORT, AppRoutes.REPORTS,
            AppRoutes.PROFILE, AppRoutes.MAP, AppRoutes.NOTIFICATIONS, "incident/test-id")) {
            compose.runOnIdle {
                assertFalse(controller.navigateIfAllowed(manager, route))
                assertEquals(AppRoutes.ADMIN, controller.currentDestination?.route)
                // Simulate a future caller that bypasses the action guard.
                controller.navigate(route)
            }
            compose.waitForIdle()
            compose.onNodeWithText("Administrador").assertIsDisplayed()
            compose.onNodeWithTag("citizen_home").assertDoesNotExist()
            compose.onNodeWithText("Reportar").assertDoesNotExist()
            compose.runOnIdle {
                assertEquals(AppRoutes.ADMIN, controller.currentDestination?.route)
                assertNull(controller.previousBackStackEntry)
            }
        }
    }

    @Test fun administratorOpensPendingManagementReturnsAndLogsOutWithoutProtectedHistory() {
        val manager = DefaultSessionManager(Storage(session("ADMIN")))
        showGate(manager)
        compose.waitUntil(5_000) { manager.state.value is SessionState.Authenticated }
        compose.onNodeWithText("Panel de administración").assertIsDisplayed()
        compose.onNodeWithText("test@example.invalid").assertIsDisplayed()
        compose.onNodeWithText("Gestión de incidentes").performClick()
        compose.onNodeWithText("Funcionalidad pendiente").assertIsDisplayed()
        compose.onNodeWithText("Volver al panel").performClick()
        compose.onNodeWithText("Panel de administración").assertIsDisplayed()
        compose.onNodeWithText("Gestión de incidentes").performClick()
        compose.onNodeWithText("Funcionalidad pendiente").assertIsDisplayed()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText("Panel de administración").assertIsDisplayed()
        compose.onNodeWithText("Cerrar sesión").performClick()
        compose.waitUntil(5_000) { manager.state.value == SessionState.Unauthenticated }
        compose.onNodeWithTag(LoginTestTags.EMAIL_FIELD).assertIsDisplayed()
        compose.onNodeWithText("Panel de administración").assertDoesNotExist()
        compose.onNodeWithText("Funcionalidad pendiente").assertDoesNotExist()
        compose.runOnIdle { assertFalse(compose.activity.onBackPressedDispatcher.hasEnabledCallbacks()) }
    }

    @Test fun invalidationWhileInPendingManagementRemovesAdministrativeNavigation() {
        val manager = DefaultSessionManager(Storage(session("ADMIN")))
        showGate(manager)
        compose.waitUntil(5_000) { manager.state.value is SessionState.Authenticated }
        compose.onNodeWithText("Gestión de incidentes").performClick()
        compose.onNodeWithText("Funcionalidad pendiente").assertIsDisplayed()
        compose.runOnIdle {
            manager.invalidateIfCurrent(requireNotNull(manager.currentTokenSnapshot()))
        }
        compose.onNodeWithTag(LoginTestTags.EMAIL_FIELD).assertIsDisplayed()
        compose.onNodeWithText("Funcionalidad pendiente").assertDoesNotExist()
        compose.runOnIdle { assertFalse(compose.activity.onBackPressedDispatcher.hasEnabledCallbacks()) }
    }

    @Test fun citizenCannotOpenAdministrativeDestinationsEvenBypassingActionGuard() {
        val manager = DefaultSessionManager(Storage(session("CIUDADANO")))
        runBlocking { manager.restore() }
        lateinit var controller: NavHostController
        compose.setContent {
            controller = rememberNavController()
            OjoCiudadanoTheme {
                AppNavigation(manager, controller, startDestination = AppRoutes.HOME)
            }
        }
        for (route in listOf(AppRoutes.ADMIN, AppRoutes.ADMIN_INCIDENTS)) {
            compose.runOnIdle {
                assertFalse(controller.navigateIfAllowed(manager, route))
                controller.navigate(route)
            }
            compose.waitForIdle()
            compose.onNodeWithText("Panel de administración").assertDoesNotExist()
            compose.onNodeWithText("Funcionalidad pendiente").assertDoesNotExist()
            compose.onNodeWithTag("citizen_home").assertIsDisplayed()
            compose.runOnIdle {
                assertEquals(AppRoutes.HOME, controller.currentDestination?.route)
                assertNull(controller.previousBackStackEntry)
            }
        }
    }

    private fun showGate(manager: SessionManager) {
        compose.setContent {
            OjoCiudadanoTheme {
                SessionStartupGate(manager, object : AuthRepository {
                    override suspend fun register(input: RegistrationInput): RegisteredUser =
                        error("Registration is not used by this test")

                    override suspend fun login(email: String, password: String): AuthenticatedSession =
                        error("Restoration must not call login")
                })
            }
        }
    }

    private class Storage(var session: AuthenticatedSession?) : SessionStorage {
        override fun currentToken() = session?.accessToken
        override fun currentSession() = session
        override fun save(session: AuthenticatedSession) { this.session = session }
        override fun clear() { session = null }
    }

    private fun session(vararg roles: String): AuthenticatedSession {
        val payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
            """{"exp":4102444800}""".toByteArray())
        return AuthenticatedSession("header.$payload.signature", "Bearer", "test-user",
            "test@example.invalid", "Test", roles.toList())
    }
}
