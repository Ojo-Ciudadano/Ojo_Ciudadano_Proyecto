package cl.uct.ojociudadano.ui.registration

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import cl.uct.ojociudadano.data.model.RegisteredUser
import cl.uct.ojociudadano.data.model.RegistrationInput
import cl.uct.ojociudadano.data.repository.AuthRepository
import cl.uct.ojociudadano.data.session.*
import cl.uct.ojociudadano.ui.login.LoginTestTags
import cl.uct.ojociudadano.ui.startup.SessionStartupGate
import cl.uct.ojociudadano.ui.theme.OjoCiudadanoTheme
import java.util.Base64
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class RegistrationNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val repository = Repository()
    private val storage = Storage()
    private val manager = DefaultSessionManager(storage)

    private fun showGate() {
        compose.setContent { OjoCiudadanoTheme { SessionStartupGate(manager, repository) } }
        compose.waitUntil(5_000) { manager.state.value == SessionState.Unauthenticated }
    }
    private fun openRegistration() {
        compose.onNodeWithText("Crear cuenta").performScrollTo().performClick()
        compose.onNodeWithTag("register_name").assertIsDisplayed()
    }
    private fun fillForm() {
        compose.onNodeWithTag("register_name").performTextInput("Ana Ciudadana")
        compose.onNodeWithTag("register_email").performTextInput("ana@example.invalid")
        compose.onNodeWithTag("register_password").performTextInput("123456")
    }
    @Test fun successfulRegistrationReturnsToLoginWithoutSessionAndExistingLoginStillWorks() {
        showGate()
        openRegistration()
        fillForm()
        compose.onNodeWithTag("register_submit").performScrollTo().performClick()
        compose.onNodeWithText("Cuenta creada. Inicia sesión con tus credenciales.").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(1, repository.registrations)
            assertEquals(0, repository.logins)
            assertEquals(0, storage.saves)
            assertEquals(SessionState.Unauthenticated, manager.state.value)
            assertNull(manager.currentToken())
            assertFalse(compose.activity.onBackPressedDispatcher.hasEnabledCallbacks())
        }
        // A new destination owns a fresh ViewModel and does not repeat stale success.
        openRegistration()
        compose.onNodeWithTag("register_name").assertTextEquals("Nombre completo", "")
        compose.onNodeWithText("Volver al inicio de sesión").performScrollTo().performClick()
        compose.onNodeWithText("Cuenta creada. Inicia sesión con tus credenciales.").assertDoesNotExist()
        compose.onNodeWithTag(LoginTestTags.EMAIL_FIELD).performTextInput("ana@example.invalid")
        compose.onNodeWithTag(LoginTestTags.PASSWORD_FIELD).performTextInput("123456")
        compose.onNodeWithTag(LoginTestTags.SUBMIT_BUTTON).performScrollTo().performClick()
        compose.waitUntil(5_000) { manager.state.value is SessionState.Authenticated }
        compose.onNodeWithTag("citizen_home").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, repository.logins); assertEquals(1, storage.saves) }
    }
    @Test fun explicitAndSystemBackReturnToLoginWithoutRequests() {
        showGate()
        openRegistration()
        compose.onNodeWithText("Volver al inicio de sesión").performScrollTo().performClick()
        compose.onNodeWithTag(LoginTestTags.EMAIL_FIELD).assertIsDisplayed()
        openRegistration()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag(LoginTestTags.EMAIL_FIELD).assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, repository.registrations); assertEquals(0, storage.saves) }
    }
    @Test fun invalidFormShowsFieldErrorsAndVisibilityToggleWorks() {
        showGate()
        openRegistration()
        compose.onNodeWithTag("register_submit").performScrollTo().performClick()
        compose.onNodeWithText("El nombre es obligatorio").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("El correo electrónico es obligatorio").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("La contraseña es obligatoria").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Mostrar contraseña").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Ocultar contraseña").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, repository.registrations) }
    }
    @Test fun loadingDisablesFormAndDuplicateSubmission() {
        val release = CompletableDeferred<Unit>()
        repository.release = release
        showGate()
        openRegistration()
        fillForm()
        compose.onNodeWithTag("register_submit").performScrollTo().performClick()
        compose.onNodeWithTag("register_loading").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("register_submit").assertIsNotEnabled()
        compose.onNodeWithTag("register_password").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(1, repository.registrations) }
        release.complete(Unit)
        compose.onNodeWithText("Cuenta creada. Inicia sesión con tus credenciales.").assertIsDisplayed()
    }
    private class Repository : AuthRepository {
        var registrations = 0
        var logins = 0
        var release: CompletableDeferred<Unit>? = null
        override suspend fun register(input: RegistrationInput): RegisteredUser {
            registrations++
            release?.await()
            return RegisteredUser("id", input.name, input.email)
        }
        override suspend fun login(email: String, password: String): AuthenticatedSession {
            logins++
            val payload = Base64.getUrlEncoder().withoutPadding().encodeToString("""{"exp":4102444800}""".toByteArray())
            return AuthenticatedSession("header.$payload.signature", "Bearer", "id", email, "Ana", listOf("CIUDADANO"))
        }
    }
    private class Storage : SessionStorage {
        var session: AuthenticatedSession? = null
        var saves = 0
        override fun currentToken() = session?.accessToken
        override fun currentSession() = session
        override fun save(session: AuthenticatedSession) { saves++; this.session = session }
        override fun clear() { session = null }
    }
}
