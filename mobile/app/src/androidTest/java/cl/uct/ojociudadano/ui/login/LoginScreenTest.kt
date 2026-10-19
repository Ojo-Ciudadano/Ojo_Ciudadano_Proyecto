package cl.uct.ojociudadano.ui.login

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.junit4.createComposeRule
import cl.uct.ojociudadano.ui.theme.OjoCiudadanoTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun screenShowsFieldsAndPasswordStartsHidden() {
        setLoginScreen()

        composeTestRule.onNodeWithTag(LoginTestTags.EMAIL_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(LoginTestTags.PASSWORD_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(LoginTestTags.SUBMIT_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Mostrar contraseña").assertIsDisplayed()
    }

    @Test
    fun passwordVisibilityCanBeToggled() {
        setLoginScreen()

        composeTestRule.onNodeWithContentDescription("Mostrar contraseña").performClick()

        composeTestRule.onNodeWithContentDescription("Ocultar contraseña").assertIsDisplayed()
    }

    @Test
    fun invalidCredentialsShowErrorsAndDoNotEmitLoginEvent() {
        var emissions = 0
        setLoginScreen { emissions += 1 }

        composeTestRule.onNodeWithTag(LoginTestTags.SUBMIT_BUTTON).performClick()

        composeTestRule.onNodeWithText("El correo electrónico es obligatorio").assertIsDisplayed()
        composeTestRule.onNodeWithText("La contraseña es obligatoria").assertIsDisplayed()
        assertEquals(0, emissions)
    }

    @Test
    fun invalidEmailDoesNotEmitLoginEvent() {
        var emissions = 0
        setLoginScreen { emissions += 1 }

        composeTestRule.onNodeWithTag(LoginTestTags.EMAIL_FIELD).performTextInput("correo-invalido")
        composeTestRule.onNodeWithTag(LoginTestTags.PASSWORD_FIELD).performTextInput("secreta")
        composeTestRule.onNodeWithTag(LoginTestTags.SUBMIT_BUTTON).performClick()

        composeTestRule.onNodeWithText("Ingresa un correo electrónico válido").assertIsDisplayed()
        assertEquals(0, emissions)
    }

    @Test
    fun emptyPasswordDoesNotEmitLoginEvent() {
        var emissions = 0
        setLoginScreen { emissions += 1 }

        composeTestRule.onNodeWithTag(LoginTestTags.EMAIL_FIELD)
            .performTextInput("ciudadano@ojociudadano.cl")
        composeTestRule.onNodeWithTag(LoginTestTags.SUBMIT_BUTTON).performClick()

        composeTestRule.onNodeWithText("La contraseña es obligatoria").assertIsDisplayed()
        assertEquals(0, emissions)
    }

    @Test
    fun validCredentialsEmitExpectedLoginEvent() {
        val emittedCredentials = mutableListOf<LoginCredentials>()
        setLoginScreen(emittedCredentials::add)

        composeTestRule.onNodeWithTag(LoginTestTags.EMAIL_FIELD)
            .performTextInput("ciudadano@ojociudadano.cl")
        composeTestRule.onNodeWithTag(LoginTestTags.PASSWORD_FIELD).performTextInput("secreta")
        composeTestRule.onNodeWithTag(LoginTestTags.SUBMIT_BUTTON).performClick()

        composeTestRule.runOnIdle {
            assertEquals(
                LoginCredentials("ciudadano@ojociudadano.cl", "secreta"),
                emittedCredentials.single()
            )
        }
        assertTrue(emittedCredentials.isNotEmpty())
    }

    @Test
    fun authenticationLoadingStateIsRendered() {
        composeTestRule.setContent {
            OjoCiudadanoTheme {
                LoginScreen(
                    onLogin = {},
                    authState = AuthUiState.Loading
                )
            }
        }

        composeTestRule.onNodeWithTag(LoginTestTags.LOADING_INDICATOR).assertIsDisplayed()
    }

    @Test
    fun authenticationErrorStateIsRendered() {
        composeTestRule.setContent {
            OjoCiudadanoTheme {
                LoginScreen(
                    onLogin = {},
                    authState = AuthUiState.Error(AuthError.Network)
                )
            }
        }

        composeTestRule.onNodeWithTag(LoginTestTags.AUTH_ERROR_MESSAGE).assertIsDisplayed()
        composeTestRule.onNodeWithText(AuthError.Network.message).assertIsDisplayed()
    }

    private fun setLoginScreen(onLogin: (LoginCredentials) -> Unit = {}) {
        composeTestRule.setContent {
            OjoCiudadanoTheme {
                LoginScreen(onLogin = onLogin)
            }
        }
    }
}
