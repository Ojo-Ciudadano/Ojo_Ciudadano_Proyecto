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

    /** Synchronizes state after an external component has invalidated persistent storage. */
    fun onStoredSessionInvalidated()
}

class DefaultSessionManager(
    private val sessionStorage: SessionStorage,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : SessionManager {

    private val stateLock = Any()
    private val _state = MutableStateFlow<SessionState>(SessionState.Restoring)

    override val state: StateFlow<SessionState> = _state.asStateFlow()

    override fun currentToken(): String? {
        return (state.value as? SessionState.Authenticated)?.session?.accessToken
    }

    override suspend fun restore(): SessionState {
        _state.value = SessionState.Restoring

        return withContext(ioDispatcher) {
            synchronized(stateLock) {
                val storedSession = runCatching { sessionStorage.currentSession() }.getOrNull()
                val nextState = storedSession
                    ?.takeIf(AuthenticatedSession::isLocallyValid)
                    ?.let(SessionState::Authenticated)
                    ?: SessionState.Unauthenticated

                if (nextState is SessionState.Unauthenticated) {
                    // A null result can be an absent, expired, corrupt, incompatible, or failed
                    // local read. In every case, do not leave recoverable credentials behind.
                    runCatching { sessionStorage.clear() }
                }

                _state.value = nextState
                nextState
            }
        }
    }

    override suspend fun establish(
        session: AuthenticatedSession
    ): SessionState.Authenticated {
        require(session.isLocallyValid()) {
            "La autenticación no devolvió una sesión válida"
        }

        return withContext(ioDispatcher) {
            synchronized(stateLock) {
                sessionStorage.save(session)
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
                    _state.value = SessionState.Unauthenticated
                }
            }
        }
    }

    override fun onStoredSessionInvalidated() {
        synchronized(stateLock) {
            // The interceptor clears the physical store first. Re-read it so a newer login that
            // won a race is never replaced by an obsolete 401 notification.
            if (runCatching { sessionStorage.currentSession() }.getOrNull() == null) {
                _state.value = SessionState.Unauthenticated
            }
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
