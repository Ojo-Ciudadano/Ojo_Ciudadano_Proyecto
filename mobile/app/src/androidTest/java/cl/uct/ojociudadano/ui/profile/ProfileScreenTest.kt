package cl.uct.ojociudadano.ui.profile

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import cl.uct.ojociudadano.data.model.UserProfile
import cl.uct.ojociudadano.ui.theme.OjoCiudadanoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProfileScreenTest {

    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val profile = UserProfile(
        id = "a0000000-0000-4000-8000-000000000001",
        name = "Ana Ciudadana",
        email = "ana@example.invalid",
        phone = "+56911111111",
        registeredAt = "2026-10-01T00:00:00.000Z",
        roles = listOf("CIUDADANO")
    )

    @Test fun loadingShowsProgressAndKeepsLogoutAvailable() {
        compose.setContent {
            OjoCiudadanoTheme {
                ProfileContent(state = ProfileUiState.Loading, onRetry = {}, onLogout = {})
            }
        }
        compose.onNodeWithText("Cargando tu perfil…").assertIsDisplayed()
        compose.onNodeWithText("Cerrar sesión").assertIsDisplayed()
        compose.onNodeWithText(profile.name).assertDoesNotExist()
    }

    @Test fun successShowsRealIdentityRoleAndAccountData() {
        compose.setContent {
            OjoCiudadanoTheme {
                ProfileContent(state = ProfileUiState.Success(profile), onRetry = {}, onLogout = {})
            }
        }
        compose.onNodeWithText("Ana Ciudadana").assertIsDisplayed()
        compose.onNodeWithText("ana@example.invalid").assertIsDisplayed()
        compose.onNodeWithText("+56911111111").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Datos de la cuenta").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Cerrar sesión").performScrollTo().assertIsDisplayed()
    }

    @Test fun absentPhoneIsShownExplicitlyInsteadOfBlank() {
        val withoutPhone = profile.copy(phone = null, registeredAt = null)
        compose.setContent {
            OjoCiudadanoTheme {
                ProfileContent(state = ProfileUiState.Success(withoutPhone), onRetry = {}, onLogout = {})
            }
        }
        compose.onNodeWithText("No registrado").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("No disponible").performScrollTo().assertIsDisplayed()
    }

    @Test fun retryableErrorOffersRetryAndInvokesIt() {
        var retries = 0
        compose.setContent {
            OjoCiudadanoTheme {
                ProfileContent(
                    state = ProfileUiState.Error("El servicio no está disponible.", canRetry = true),
                    onRetry = { retries++ },
                    onLogout = {}
                )
            }
        }
        compose.onNodeWithText("El servicio no está disponible.").assertIsDisplayed()
        compose.onNodeWithText("Reintentar").performClick()
        assertEquals(1, retries)
        compose.onNodeWithText("Cerrar sesión").performScrollTo().assertIsDisplayed()
    }

    @Test fun terminalErrorHidesRetryButKeepsLogout() {
        compose.setContent {
            OjoCiudadanoTheme {
                ProfileContent(
                    state = ProfileUiState.Error("Tu sesión expiró. Inicia sesión nuevamente.", canRetry = false),
                    onRetry = {},
                    onLogout = {}
                )
            }
        }
        compose.onNodeWithText("Reintentar").assertDoesNotExist()
        compose.onNodeWithText("Cerrar sesión").performScrollTo().assertIsDisplayed()
    }
}
