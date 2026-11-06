package cl.uct.ojociudadano.data.session

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/** The single observable source of truth for the app authentication state. */
interface SessionManager : TokenProvider {

    val state: StateFlow<SessionState>

    /** Restores a locally valid persisted session, if one exists. */
    suspend fun restore(): SessionState

    /** Persists a successful login before exposing it as authenticated. */
    suspend fun establish(session: AuthenticatedSession): SessionState.Authenticated

    /** Clears persistent data and immediately exposes an unauthenticated state. */
    suspend fun logout()

    /** Invalidates only the session that issued a request rejected with HTTP 401. */
    fun invalidateIfCurrent(rejectedToken: TokenSnapshot)
}

class DefaultSessionManager(
    private val sessionStorage: SessionStorage,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val nowEpochSeconds: () -> Long = { System.currentTimeMillis() / 1000 }
) : SessionManager {

    private val stateLock = Any()
    private val _state = MutableStateFlow<SessionState>(SessionState.Restoring)
    private var sessionGeneration = 0L

    override val state: StateFlow<SessionState> = _state.asStateFlow()

    override fun currentToken(): String? {
        return currentTokenSnapshot()?.value
    }

    override fun currentTokenSnapshot(): TokenSnapshot? {
        return synchronized(stateLock) {
            val session = (_state.value as? SessionState.Authenticated)?.session
                ?: return@synchronized null

            if (session.isLocallyValid(nowEpochSeconds())) {
                TokenSnapshot(
                    value = session.accessToken,
                    sessionGeneration = sessionGeneration
                )
            } else {
                runCatching { sessionStorage.clear() }
                sessionGeneration += 1
                _state.value = SessionState.Unauthenticated
                null
            }
        }
    }

    override suspend fun restore(): SessionState {
        _state.value = SessionState.Restoring

        return withContext(ioDispatcher) {
            synchronized(stateLock) {
                val storedSession = runCatching { sessionStorage.currentSession() }.getOrNull()
                val nextState = storedSession
                    ?.takeIf { it.isLocallyValid(nowEpochSeconds()) }
                    ?.let(SessionState::Authenticated)
                    ?: SessionState.Unauthenticated

                if (nextState is SessionState.Unauthenticated) {
                    // A null result can be an absent, expired, corrupt, incompatible, or failed
                    // local read. In every case, do not leave recoverable credentials behind.
                    runCatching { sessionStorage.clear() }
                }

                sessionGeneration += 1
                _state.value = nextState
                nextState
            }
        }
    }

    override suspend fun establish(
        session: AuthenticatedSession
    ): SessionState.Authenticated {
        require(session.isLocallyValid(nowEpochSeconds())) {
            "La autenticación no devolvió una sesión válida"
        }

        return withContext(ioDispatcher) {
            synchronized(stateLock) {
                sessionStorage.save(session)
                sessionGeneration += 1
                SessionState.Authenticated(session).also { _state.value = it }
            }
        }
    }

    override suspend fun logout() {
        withContext(ioDispatcher) {
            synchronized(stateLock) {
                try {
                    sessionStorage.clear()
                } finally {
                    sessionGeneration += 1
                    _state.value = SessionState.Unauthenticated
                }
            }
        }
    }

    override fun invalidateIfCurrent(rejectedToken: TokenSnapshot) {
        if (rejectedToken.value.isBlank()) return

        synchronized(stateLock) {
            val activeSession = (_state.value as? SessionState.Authenticated)?.session
                ?: return@synchronized

            // A delayed 401 from an older request must not clear a session established later.
            if (
                activeSession.accessToken != rejectedToken.value ||
                rejectedToken.sessionGeneration != sessionGeneration
            ) {
                return@synchronized
            }

            runCatching { sessionStorage.clear() }
            sessionGeneration += 1
            _state.value = SessionState.Unauthenticated
        }
    }
}

sealed interface SessionState {

    data object Restoring : SessionState

    data class Authenticated(
        val session: AuthenticatedSession
    ) : SessionState

    data object Unauthenticated : SessionState
}
