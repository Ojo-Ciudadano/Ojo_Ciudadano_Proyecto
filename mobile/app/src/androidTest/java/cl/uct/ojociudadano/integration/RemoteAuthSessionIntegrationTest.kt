package cl.uct.ojociudadano.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cl.uct.ojociudadano.data.session.DefaultSessionManager
import cl.uct.ojociudadano.data.session.SessionState
import cl.uct.ojociudadano.di.AppContainer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Optional end-to-end test against the development API. It is skipped unless the runner receives
 * `authEmail` and `authPassword` arguments, keeping credentials out of source control and Gradle
 * configuration.
 */
@RunWith(AndroidJUnit4::class)
class RemoteAuthSessionIntegrationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val container = AppContainer(context)

    @After
    fun tearDown() = runBlocking {
        container.sessionManager.logout()
    }

    @Test
    fun loginPersistsRestoresAndLogsOutUsingTheDevelopmentApi() = runBlocking {
        val arguments = InstrumentationRegistry.getArguments()
        val email = arguments.getString(ARGUMENT_EMAIL)
        val password = arguments.getString(ARGUMENT_PASSWORD)

        assumeTrue(
            "Requires authEmail and authPassword instrumentation arguments",
            !email.isNullOrBlank() && !password.isNullOrBlank()
        )
        val suppliedEmail = requireNotNull(email)
        val suppliedPassword = requireNotNull(password)

        val session = container.authRepository.login(suppliedEmail, suppliedPassword)
        container.sessionManager.establish(session)

        assertTrue(container.sessionManager.currentToken() == session.accessToken)
        assertTrue(container.sessionManager.state.value is SessionState.Authenticated)

        val recoveredManager = DefaultSessionManager(container.sessionStorage)
        recoveredManager.restore()
        assertTrue(recoveredManager.currentToken() == session.accessToken)
        assertTrue(recoveredManager.state.value is SessionState.Authenticated)

        container.sessionManager.logout()
        assertTrue(container.sessionManager.state.value is SessionState.Unauthenticated)

        val managerAfterLogout = DefaultSessionManager(container.sessionStorage)
        managerAfterLogout.restore()
        assertTrue(managerAfterLogout.state.value is SessionState.Unauthenticated)
    }

    private companion object {
        const val ARGUMENT_EMAIL = "authEmail"
        const val ARGUMENT_PASSWORD = "authPassword"
    }
}
