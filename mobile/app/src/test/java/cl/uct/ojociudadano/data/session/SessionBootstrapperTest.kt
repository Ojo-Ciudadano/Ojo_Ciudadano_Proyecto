package cl.uct.ojociudadano.data.session

import cl.uct.ojociudadano.data.repository.AuthRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionBootstrapperTest {

    @Test
    fun `bootstrap logs in and persists session when no session exists`() = runTest {
        val sessionStorage = FakeSessionStorage()
        val authRepository = FakeAuthRepository(
            sessionStorage = sessionStorage,
            loginOutcomes = mutableListOf(Result.success(sampleSession))
        )
        val bootstrapper = SessionBootstrapper(
            sessionStorage = sessionStorage,
            authRepository = authRepository,
            developmentCredentials = configuredCredentials
        )

        val state = bootstrapper.bootstrap()

        assertEquals(1, authRepository.loginCalls)
        assertEquals(sampleSession, sessionStorage.currentSession())
        assertEquals(
            SessionBootstrapState.Authenticated(sampleSession),
            state
        )
    }

    @Test
    fun `bootstrap reuses existing session without logging in`() = runTest {
        val sessionStorage = FakeSessionStorage(sampleSession)
        val authRepository = FakeAuthRepository(
            sessionStorage = sessionStorage,
            loginOutcomes = mutableListOf(Result.success(sampleSession))
        )
        val bootstrapper = SessionBootstrapper(
            sessionStorage = sessionStorage,
            authRepository = authRepository,
            developmentCredentials = configuredCredentials
        )

        val state = bootstrapper.bootstrap()

        assertEquals(0, authRepository.loginCalls)
        assertEquals(
            SessionBootstrapState.Authenticated(sampleSession),
            state
        )
    }

    @Test
    fun `bootstrap returns error when login fails`() = runTest {
        val sessionStorage = FakeSessionStorage()
        val authRepository = FakeAuthRepository(
            sessionStorage = sessionStorage,
            loginOutcomes = mutableListOf(
                Result.failure(IllegalStateException("login test failure"))
            )
        )
        val bootstrapper = SessionBootstrapper(
            sessionStorage = sessionStorage,
            authRepository = authRepository,
            developmentCredentials = configuredCredentials
        )

        val state = bootstrapper.bootstrap()

        assertEquals(1, authRepository.loginCalls)
        assertTrue(state is SessionBootstrapState.Error)
        assertEquals(null, sessionStorage.currentSession())
    }

    @Test
    fun `retrying bootstrap attempts login again after failure`() = runTest {
        val sessionStorage = FakeSessionStorage()
        val authRepository = FakeAuthRepository(
            sessionStorage = sessionStorage,
            loginOutcomes = mutableListOf(
                Result.failure(IllegalStateException("login test failure")),
                Result.success(sampleSession)
            )
        )
        val bootstrapper = SessionBootstrapper(
            sessionStorage = sessionStorage,
            authRepository = authRepository,
            developmentCredentials = configuredCredentials
        )

        val firstAttempt = bootstrapper.bootstrap()
        val retryAttempt = bootstrapper.bootstrap()

        assertTrue(firstAttempt is SessionBootstrapState.Error)
        assertEquals(
            SessionBootstrapState.Authenticated(sampleSession),
            retryAttempt
        )
        assertEquals(2, authRepository.loginCalls)
        assertEquals(sampleSession, sessionStorage.currentSession())
    }

    private class FakeAuthRepository(
        private val sessionStorage: SessionStorage,
        private val loginOutcomes: MutableList<Result<AuthenticatedSession>>
    ) : AuthRepository {

        var loginCalls = 0
            private set

        override suspend fun login(
            email: String,
            password: String
        ): AuthenticatedSession {
            loginCalls += 1
            val outcome = loginOutcomes.removeAt(0)

            return outcome.getOrElse { throw it }
                .also(sessionStorage::save)
        }
    }

    private class FakeSessionStorage(
        private var session: AuthenticatedSession? = null
    ) : SessionStorage {

        override fun currentToken(): String? = session?.accessToken

        override fun currentSession(): AuthenticatedSession? = session

        override fun save(session: AuthenticatedSession) {
            this.session = session
        }

        override fun clear() {
            session = null
        }
    }

    private companion object {
        val configuredCredentials = DevelopmentAuthCredentials(
            email = "developer@example.invalid",
            password = "not-a-real-password"
        )

        val sampleSession = AuthenticatedSession(
            accessToken = "test-access-token",
            tokenType = "Bearer",
            userId = "test-user-id",
            email = "developer@example.invalid",
            name = "Development User",
            roles = listOf("CIUDADANO")
        )
    }
}
