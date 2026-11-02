package cl.uct.ojociudadano.ui.reports

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import cl.uct.ojociudadano.ui.home.HomeScreen
import cl.uct.ojociudadano.ui.login.LoginScreen
import cl.uct.ojociudadano.ui.login.LoginTestTags
import cl.uct.ojociudadano.ui.registration.RegistrationScreen
import cl.uct.ojociudadano.ui.registration.RegistrationUiState
import cl.uct.ojociudadano.ui.theme.OjoCiudadanoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Fixtures are used only in tests; narrow layout exercises a 320dp portrait viewport. */
class CitizenPresentationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun smallLoginWithKeyboardCanReachSubmitAndTogglePassword() {
        compose.setContent { OjoCiudadanoTheme { Box(Modifier.width(320.dp)) { LoginScreen(onLogin = {}) } } }
        compose.onNodeWithTag(LoginTestTags.EMAIL_FIELD).performTextInput("test@example.invalid")
        compose.onNodeWithTag(LoginTestTags.PASSWORD_FIELD).performScrollTo().performTextInput("123456")
        compose.onNodeWithContentDescription("Mostrar contraseña").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Ocultar contraseña").assertIsDisplayed()
        compose.onNodeWithTag(LoginTestTags.SUBMIT_BUTTON).performScrollTo().assertIsDisplayed().performClick()
        capture("citizen-login")
    }

    @Test fun smallRegistrationKeepsReturnAndValidationReachable() {
        var backs = 0
        compose.setContent { OjoCiudadanoTheme { Box(Modifier.width(320.dp)) {
            RegistrationScreen(RegistrationUiState.Idle, {}, { backs++ })
        } } }
        compose.onNodeWithTag("register_phone").performScrollTo().performTextInput("+56912345678")
        compose.onNodeWithContentDescription("Mostrar contraseña").performScrollTo().performClick()
        compose.onNodeWithText("Volver al inicio de sesión").performScrollTo().assertIsDisplayed().performClick()
        assertEquals(1, backs)
        capture("citizen-registration")
    }

    @Test fun smallHomeUsesSessionNameAndDoesNotPresentFailedCountsAsZero() {
        var reports = 0
        compose.setContent { OjoCiudadanoTheme { Box(Modifier.width(320.dp)) {
            HomeScreen(recentIncidents = emptyList(), incidentsUnderReview = 0, incidentsInViewOf = 0, solvedIncidents = 0,
                name = "Ana Ciudadana", state = IncidentsUiState.Error("Servicio no disponible"), onReportsClick = { reports++ })
        } } }
        compose.onNodeWithText("Hola, Ana").assertIsDisplayed()
        compose.onNodeWithText("0").assertDoesNotExist()
        compose.onNodeWithText("Ver todos").performScrollTo().performClick()
        assertEquals(1, reports)
        compose.onNodeWithText("Servicio no disponible").performScrollTo().assertIsDisplayed()
        capture("citizen-home")
    }

    private fun capture(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        java.io.File(compose.activity.cacheDir, "$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
