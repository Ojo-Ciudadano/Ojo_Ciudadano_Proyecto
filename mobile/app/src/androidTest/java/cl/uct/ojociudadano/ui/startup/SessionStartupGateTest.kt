package cl.uct.ojociudadano.ui.startup

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import cl.uct.ojociudadano.data.repository.AuthRepository
import cl.uct.ojociudadano.data.session.AuthenticatedSession
import cl.uct.ojociudadano.data.session.DefaultSessionManager
import cl.uct.ojociudadano.data.session.SessionStorage
import cl.uct.ojociudadano.ui.login.LoginTestTags
import cl.uct.ojociudadano.ui.theme.OjoCiudadanoTheme
import org.junit.Rule
import org.junit.Test

class SessionStartupGateTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun noPersistedSessionShowsLoginWithoutCallingRepository() {
        val repository = FailingAuthRepository()
        val sessionManager = DefaultSessionManager(EmptySessionStorage())

        composeTestRule.setContent {
            OjoCiudadanoTheme {
                SessionStartupGate(
                    sessionManager = sessionManager,
                    authRepository = repository
                )
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag(LoginTestTags.EMAIL_FIELD)
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag(LoginTestTags.EMAIL_FIELD).assertIsDisplayed()
        check(repository.loginCalls == 0)
    }

    private class FailingAuthRepository : AuthRepository {
        var loginCalls = 0

        override suspend fun login(email: String, password: String): AuthenticatedSession {
            loginCalls += 1
            error("The normal startup flow must not authenticate automatically")
        }
    }

    private class EmptySessionStorage : SessionStorage {
        override fun currentToken(): String? = null

        override fun currentSession(): AuthenticatedSession? = null

        override fun save(session: AuthenticatedSession) = Unit

        override fun clear() = Unit
    }
}
