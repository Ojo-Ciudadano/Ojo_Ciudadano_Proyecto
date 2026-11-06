package cl.uct.ojociudadano.ui.login

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import cl.uct.ojociudadano.data.repository.AuthRepository
import cl.uct.ojociudadano.data.session.AuthenticatedSession
import cl.uct.ojociudadano.data.session.SessionManager
import cl.uct.ojociudadano.data.session.SessionState
import cl.uct.ojociudadano.data.session.TokenSnapshot
import cl.uct.ojociudadano.ui.theme.OjoCiudadanoTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import cl.uct.ojociudadano.data.model.RegisteredUser
import cl.uct.ojociudadano.data.model.RegistrationInput

class LoginRouteTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun validLoginScreenEventInvokesAuthViewModelRepositoryFlow() {
        val repository = RecordingAuthRepository()
        val viewModel = AuthViewModel(repository, RecordingSessionManager())

        composeTestRule.setContent {
            OjoCiudadanoTheme {
                LoginRoute(viewModel = viewModel)
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
        override suspend fun register(input: RegistrationInput): RegisteredUser =
            error("Registration is not used by this test")

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

    private class RecordingSessionManager : SessionManager {

        private val _state = MutableStateFlow<SessionState>(SessionState.Unauthenticated)
        override val state: StateFlow<SessionState> = _state.asStateFlow()

        override fun currentToken(): String? =
            (state.value as? SessionState.Authenticated)?.session?.accessToken

        override suspend fun restore(): SessionState = state.value

        override suspend fun establish(session: AuthenticatedSession): SessionState.Authenticated {
            return SessionState.Authenticated(session).also { _state.value = it }
        }

        override suspend fun logout() {
            _state.value = SessionState.Unauthenticated
        }

        override fun invalidateIfCurrent(rejectedToken: TokenSnapshot) {
            if (currentToken() == rejectedToken.value) {
                _state.value = SessionState.Unauthenticated
            }
        }
    }
}
