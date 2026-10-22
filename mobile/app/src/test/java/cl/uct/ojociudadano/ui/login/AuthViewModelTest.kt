package cl.uct.ojociudadano.ui.login

import cl.uct.ojociudadano.data.remote.error.ApiException
import cl.uct.ojociudadano.data.repository.AuthRepository
import cl.uct.ojociudadano.data.session.AuthenticatedSession
import cl.uct.ojociudadano.data.session.SessionManager
import cl.uct.ojociudadano.data.session.SessionState
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is idle`() {
        val viewModel = AuthViewModel(FakeAuthRepository(), FakeSessionManager())

        assertEquals(AuthUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `login enters loading and forwards entered credentials to repository`() = runTest {
        val loginStarted = CompletableDeferred<Unit>()
        val releaseLogin = CompletableDeferred<Unit>()
        val repository = FakeAuthRepository(
            onLogin = {
                loginStarted.complete(Unit)
                releaseLogin.await()
                sampleSession
            }
        )
        val viewModel = AuthViewModel(repository, FakeSessionManager())

        viewModel.login(
            LoginCredentials(
                email = " ciudadano@ojociudadano.cl ",
                password = "password-not-stored"
            )
        )
        loginStarted.await()

        assertEquals(AuthUiState.Loading, viewModel.uiState.value)
        assertEquals(
            LoginCredentials("ciudadano@ojociudadano.cl", "password-not-stored"),
            repository.loginCredentials.single()
        )

        releaseLogin.complete(Unit)

        assertEquals(AuthUiState.Success(sampleSession), viewModel.uiState.value)
    }

    @Test
    fun `successful login exposes the authenticated session`() = runTest {
        val sessionManager = FakeSessionManager()
        val viewModel = AuthViewModel(
            FakeAuthRepository(onLogin = { sampleSession }),
            sessionManager
        )

        viewModel.login("ciudadano@ojociudadano.cl", "password-not-stored")

        val state = viewModel.uiState.value
        assertTrue(state is AuthUiState.Success)
        assertEquals(sampleSession, (state as AuthUiState.Success).session)
        assertEquals(sampleSession, sessionManager.establishedSession)
        assertEquals(
            SessionState.Authenticated(sampleSession),
            sessionManager.state.value
        )
    }

    @Test
    fun `http 401 becomes invalid credentials error`() = runTest {
        val viewModel = errorViewModel(
            ApiException.Http(
                statusCode = 401,
                serverMessages = listOf("Credenciales inválidas"),
                error = "Unauthorized",
                path = "/api/v1/auth/login"
            )
        )

        assertEquals(
            AuthUiState.Error(AuthError.InvalidCredentials),
            viewModel.uiState.value
        )
    }

    @Test
    fun `http validation error keeps the backend validation message`() = runTest {
        val viewModel = errorViewModel(
            ApiException.Http(
                statusCode = 400,
                serverMessages = listOf("password must be longer than or equal to 4 characters"),
                error = "Bad Request",
                path = "/api/v1/auth/login"
            )
        )

        assertEquals(
            AuthUiState.Error(
                AuthError.Validation(
                    "password must be longer than or equal to 4 characters"
                )
            ),
            viewModel.uiState.value
        )
    }

    @Test
    fun `server http error is controlled`() = runTest {
        val viewModel = errorViewModel(
            ApiException.Http(
                statusCode = 503,
                serverMessages = emptyList(),
                error = "Service Unavailable",
                path = "/api/v1/auth/login"
            )
        )

        assertEquals(AuthUiState.Error(AuthError.Server), viewModel.uiState.value)
    }

    @Test
    fun `network error is controlled`() = runTest {
        val viewModel = errorViewModel(ApiException.Network(IOException("offline")))

        assertEquals(AuthUiState.Error(AuthError.Network), viewModel.uiState.value)
    }

    @Test
    fun `unexpected exception does not leave state in loading`() = runTest {
        val viewModel = errorViewModel(IllegalStateException("not shown to the user"))

        assertEquals(AuthUiState.Error(AuthError.Unexpected), viewModel.uiState.value)
    }

    @Test
    fun `session persistence failure is controlled after a successful remote login`() = runTest {
        val viewModel = AuthViewModel(
            FakeAuthRepository(onLogin = { sampleSession }),
            FakeSessionManager(establishFailure = IllegalStateException("storage failed"))
        )

        viewModel.login("ciudadano@ojociudadano.cl", "password-not-stored")

        assertEquals(AuthUiState.Error(AuthError.Unexpected), viewModel.uiState.value)
    }

    @Test
    fun `a new attempt replaces an error and can succeed`() = runTest {
        val repository = FakeAuthRepository(
            outcomes = ArrayDeque(
                listOf(
                    Result.failure(ApiException.Network(IOException("offline"))),
                    Result.success(sampleSession)
                )
            )
        )
        val viewModel = AuthViewModel(repository, FakeSessionManager())

        viewModel.login("ciudadano@ojociudadano.cl", "password-not-stored")
        assertEquals(AuthUiState.Error(AuthError.Network), viewModel.uiState.value)

        viewModel.login("ciudadano@ojociudadano.cl", "password-not-stored")

        assertEquals(AuthUiState.Success(sampleSession), viewModel.uiState.value)
        assertEquals(2, repository.loginCredentials.size)
    }

    @Test
    fun `duplicate login requests are ignored while one is loading`() = runTest {
        val loginStarted = CompletableDeferred<Unit>()
        val releaseLogin = CompletableDeferred<Unit>()
        val repository = FakeAuthRepository(
            onLogin = {
                loginStarted.complete(Unit)
                releaseLogin.await()
                sampleSession
            }
        )
        val viewModel = AuthViewModel(repository, FakeSessionManager())

        viewModel.login("ciudadano@ojociudadano.cl", "password-not-stored")
        loginStarted.await()
        viewModel.login("otra@ojociudadano.cl", "different-password")

        assertEquals(AuthUiState.Loading, viewModel.uiState.value)
        assertEquals(
            listOf(LoginCredentials("ciudadano@ojociudadano.cl", "password-not-stored")),
            repository.loginCredentials
        )

        releaseLogin.complete(Unit)
        assertEquals(AuthUiState.Success(sampleSession), viewModel.uiState.value)
    }

    private fun errorViewModel(error: Throwable): AuthViewModel {
        return AuthViewModel(
            FakeAuthRepository(onLogin = { throw error }),
            FakeSessionManager()
        ).also { viewModel ->
            viewModel.login("ciudadano@ojociudadano.cl", "password-not-stored")
        }
    }
}

private class FakeSessionManager(
    private val establishFailure: Throwable? = null
) : SessionManager {

    private val _state = MutableStateFlow<SessionState>(SessionState.Unauthenticated)
    override val state: StateFlow<SessionState> = _state.asStateFlow()

    var establishedSession: AuthenticatedSession? = null
        private set

    override fun currentToken(): String? = establishedSession?.accessToken

    override suspend fun restore(): SessionState = state.value

    override suspend fun establish(session: AuthenticatedSession): SessionState.Authenticated {
        establishFailure?.let { throw it }
        establishedSession = session
        return SessionState.Authenticated(session).also { _state.value = it }
    }

    override suspend fun logout() {
        establishedSession = null
        _state.value = SessionState.Unauthenticated
    }

    override fun onStoredSessionInvalidated() {
        establishedSession = null
        _state.value = SessionState.Unauthenticated
    }
}

private class FakeAuthRepository(
    private val outcomes: ArrayDeque<Result<AuthenticatedSession>> = ArrayDeque(),
    private val onLogin: suspend (LoginCredentials) -> AuthenticatedSession = {
        outcomes.removeFirst().getOrElse { throw it }
    }
) : AuthRepository {

    val loginCredentials = mutableListOf<LoginCredentials>()

    override suspend fun login(email: String, password: String): AuthenticatedSession {
        val credentials = LoginCredentials(email = email, password = password)
        loginCredentials += credentials
        return onLogin(credentials)
    }
}

private val sampleSession = AuthenticatedSession(
    accessToken = "token-not-rendered",
    tokenType = "Bearer",
    userId = "test-user-id",
    email = "ciudadano@ojociudadano.cl",
    name = "Ciudadano de prueba",
    roles = listOf("CIUDADANO")
)
