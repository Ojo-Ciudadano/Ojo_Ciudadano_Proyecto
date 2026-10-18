package cl.uct.ojociudadano.data.session

import cl.uct.ojociudadano.data.repository.AuthRepository
import java.util.Base64
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    @Test
    fun `bootstrap clears expired JWT session and logs in again`() = runTest {
        val expiredSession = sampleSession.copy(
            accessToken = jwtWithPayload("""{"sub":"u1","exp":1000}""")
        )
        val sessionStorage = FakeSessionStorage(expiredSession)
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
        assertEquals(SessionBootstrapState.Authenticated(sampleSession), state)
    }

    @Test
    fun `isExpired fails closed on malformed token or token without exp`() {
        val malformedSession = sampleSession.copy(accessToken = "not-a-jwt")
        val invalidBase64Session = sampleSession.copy(accessToken = "a.%%%invalid%%%.c")
        val missingExpSession = sampleSession.copy(
            accessToken = jwtWithPayload("""{"sub":"u1"}""")
        )

        assertTrue(malformedSession.isExpired(nowEpochSeconds = 2000))
        assertTrue(invalidBase64Session.isExpired(nowEpochSeconds = 2000))
        assertTrue(missingExpSession.isExpired(nowEpochSeconds = 2000))
        assertFalse(sampleSession.isExpired(nowEpochSeconds = 2000))
    }

    @Test
    fun `bootstrap clears malformed JWT session and logs in again`() = runTest {
        val malformedSession = sampleSession.copy(accessToken = "malformed-token")
        val sessionStorage = FakeSessionStorage(malformedSession)
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
        assertEquals(SessionBootstrapState.Authenticated(sampleSession), state)
    }

    @Test
    fun `release mode requests interactive login and recovers session after 401 invalidation`() = runTest {
        val sessionStorage = FakeSessionStorage()
        val authRepository = FakeAuthRepository(
            sessionStorage = sessionStorage,
            loginOutcomes = mutableListOf(Result.success(sampleSession))
        )
        val bootstrapper = SessionBootstrapper(
            sessionStorage = sessionStorage,
            authRepository = authRepository,
            developmentCredentials = configuredCredentials,
            allowDevelopmentAutoLogin = false
        )

        val initialState = bootstrapper.bootstrap()
        assertEquals(0, authRepository.loginCalls)
        assertEquals(SessionBootstrapState.RequiresLogin(), initialState)

        val authenticatedState = bootstrapper.login("ciudadano@ojociudadano.cl", "Secret123!")
        assertEquals(1, authRepository.loginCalls)
        assertEquals(SessionBootstrapState.Authenticated(sampleSession), authenticatedState)
        assertEquals(sampleSession, sessionStorage.currentSession())

        bootstrapper.onSessionInvalidated()
        assertNull(sessionStorage.currentSession())
        assertTrue(bootstrapper.state.value is SessionBootstrapState.RequiresLogin)
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
        fun jwtWithPayload(json: String): String {
            val payload = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(json.toByteArray())
            return "eyJhbGciOiJIUzI1NiJ9.$payload.sig"
        }

        val configuredCredentials = DevelopmentAuthCredentials(
            email = "developer@example.invalid",
            password = "not-a-real-password"
        )

        val sampleSession = AuthenticatedSession(
            accessToken = jwtWithPayload("""{"sub":"test-user-id","exp":4102444800}"""),
            tokenType = "Bearer",
            userId = "test-user-id",
            email = "developer@example.invalid",
            name = "Development User",
            roles = listOf("CIUDADANO")
        )
    }
}
