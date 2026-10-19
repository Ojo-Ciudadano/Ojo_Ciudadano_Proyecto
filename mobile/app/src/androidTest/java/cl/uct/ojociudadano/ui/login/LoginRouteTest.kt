package cl.uct.ojociudadano.ui.login

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import cl.uct.ojociudadano.data.repository.AuthRepository
import cl.uct.ojociudadano.data.session.AuthenticatedSession
import cl.uct.ojociudadano.ui.theme.OjoCiudadanoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LoginRouteTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun validLoginScreenEventInvokesAuthViewModelRepositoryFlow() {
        val repository = RecordingAuthRepository()
        val viewModel = AuthViewModel(repository)

        composeTestRule.setContent {
            OjoCiudadanoTheme {
                LoginRoute(
                    viewModel = viewModel,
                    onAuthenticated = {}
                )
            }
        }

        composeTestRule.onNodeWithTag(LoginTestTags.EMAIL_FIELD)
            .performTextInput("ciudadano@ojociudadano.cl")
        composeTestRule.onNodeWithTag(LoginTestTags.PASSWORD_FIELD)
            .performTextInput("password-not-stored")
        composeTestRule.onNodeWithTag(LoginTestTags.SUBMIT_BUTTON).performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            repository.loginCredentials.isNotEmpty()
        }

        assertEquals(
            listOf(LoginCredentials("ciudadano@ojociudadano.cl", "password-not-stored")),
            repository.loginCredentials
        )
    }

    private class RecordingAuthRepository : AuthRepository {

        val loginCredentials = mutableListOf<LoginCredentials>()

        override suspend fun login(email: String, password: String): AuthenticatedSession {
            loginCredentials += LoginCredentials(email, password)
            return AuthenticatedSession(
                accessToken = "token-not-rendered",
                tokenType = "Bearer",
                userId = "test-user-id",
                email = email,
                name = "Ciudadano de prueba",
                roles = listOf("CIUDADANO")
            )
        }
    }
}
