package cl.uct.ojociudadano.data.session

import java.util.Base64
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionManagerTest {

    @Test
    fun `successful login persists session then exposes authenticated state`() = runTest {
        val storage = InMemorySessionStorage()
        val manager = DefaultSessionManager(storage)

        val state = manager.establish(validSession)

        assertEquals(SessionState.Authenticated(validSession), state)
        assertEquals(SessionState.Authenticated(validSession), manager.state.value)
        assertEquals(validSession, storage.currentSession())
        assertEquals(validSession.accessToken, manager.currentToken())
    }

    @Test
    fun `app restart restores a valid persisted session`() = runTest {
        val storage = InMemorySessionStorage(validSession)
        val manager = DefaultSessionManager(storage)

        val state = manager.restore()

        assertEquals(SessionState.Authenticated(validSession), state)
        assertEquals(validSession.accessToken, manager.currentToken())
    }

    @Test
    fun `no persisted session becomes unauthenticated after recovery`() = runTest {
        val manager = DefaultSessionManager(InMemorySessionStorage())

        assertEquals(SessionState.Restoring, manager.state.value)
        assertEquals(SessionState.Unauthenticated, manager.restore())
        assertNull(manager.currentToken())
    }

    @Test
    fun `expired JWT is cleared and never restored`() = runTest {
        val storage = InMemorySessionStorage(
            validSession.copy(accessToken = jwtWithPayload("""{"sub":"u1","exp":1000}"""))
        )
        val manager = DefaultSessionManager(storage)

        assertEquals(SessionState.Unauthenticated, manager.restore())
        assertNull(storage.currentSession())
        assertTrue(storage.clearCalls > 0)
    }

    @Test
    fun `JWT that expires while app is running is not exposed`() = runTest {
        var nowEpochSeconds = 1_000L
        val storage = InMemorySessionStorage()
        val manager = DefaultSessionManager(
            sessionStorage = storage,
            nowEpochSeconds = { nowEpochSeconds }
        )
        val shortSession = validSession.copy(
            accessToken = jwtWithPayload("""{"sub":"u1","exp":1001}""")
        )
        manager.establish(shortSession)

        nowEpochSeconds = 1_001L

        assertNull(manager.currentToken())
        assertEquals(SessionState.Unauthenticated, manager.state.value)
        assertNull(storage.currentSession())
    }

    @Test
    fun `corrupt storage has a controlled unauthenticated result`() = runTest {
        val storage = InMemorySessionStorage(readFailure = IllegalStateException("corrupt payload"))
        val manager = DefaultSessionManager(storage)

        assertEquals(SessionState.Unauthenticated, manager.restore())
        assertEquals(SessionState.Unauthenticated, manager.state.value)
        assertTrue(storage.clearCalls > 0)
    }

    @Test
    fun `logout clears persisted state and cannot be restored after restart`() = runTest {
        val storage = InMemorySessionStorage()
        val manager = DefaultSessionManager(storage)
        manager.establish(validSession)

        manager.logout()

        assertEquals(SessionState.Unauthenticated, manager.state.value)
        assertNull(manager.currentToken())
        assertNull(storage.currentSession())
        assertEquals(SessionState.Unauthenticated, DefaultSessionManager(storage).restore())
    }

    @Test
    fun `invalid login payload is not persisted`() = runTest {
        val storage = InMemorySessionStorage()
        val manager = DefaultSessionManager(storage)

        val result = runCatching {
            manager.establish(validSession.copy(accessToken = "not-a-jwt"))
        }

        assertTrue(result.isFailure)
        assertNull(storage.currentSession())
        assertFalse(manager.state.value is SessionState.Authenticated)
    }

    @Test
    fun `storage write failure never exposes an authenticated session`() = runTest {
        val storage = InMemorySessionStorage(writeFailure = IllegalStateException("disk full"))
        val manager = DefaultSessionManager(storage)
        manager.restore()

        val result = runCatching { manager.establish(validSession) }

        assertTrue(result.isFailure)
        assertEquals(SessionState.Unauthenticated, manager.state.value)
        assertNull(manager.currentToken())
    }

    @Test
    fun `401 for current token clears persisted and observable session`() = runTest {
        val storage = InMemorySessionStorage()
        val manager = DefaultSessionManager(storage)
        manager.establish(validSession)
        val rejectedToken = requireNotNull(manager.currentTokenSnapshot())

        manager.invalidateIfCurrent(rejectedToken)

        assertEquals(SessionState.Unauthenticated, manager.state.value)
        assertNull(manager.currentToken())
        assertNull(storage.currentSession())
        assertEquals(1, storage.clearCalls)
    }

    @Test
    fun `delayed 401 for old token preserves newer session`() = runTest {
        val storage = InMemorySessionStorage()
        val manager = DefaultSessionManager(storage)
        val oldSession = validSession.copy(
            accessToken = jwtWithPayload("""{"sub":"old","exp":4102444800}"""),
            userId = "old"
        )
        val newSession = oldSession.copy(name = "New login, same JWT")
        manager.establish(oldSession)
        val rejectedToken = requireNotNull(manager.currentTokenSnapshot())
        manager.establish(newSession)

        manager.invalidateIfCurrent(rejectedToken)

        assertEquals(SessionState.Authenticated(newSession), manager.state.value)
        assertEquals(newSession.accessToken, manager.currentToken())
        assertEquals(newSession, storage.currentSession())
        assertEquals(0, storage.clearCalls)
    }

    private class InMemorySessionStorage(
        initialSession: AuthenticatedSession? = null,
        private val readFailure: Throwable? = null,
        private val writeFailure: Throwable? = null
    ) : SessionStorage {

        private var session: AuthenticatedSession? = initialSession
        var clearCalls = 0
            private set

        override fun currentToken(): String? = currentSession()?.accessToken

        override fun currentSession(): AuthenticatedSession? {
            readFailure?.let { throw it }
            return session
        }

        override fun save(session: AuthenticatedSession) {
            writeFailure?.let { throw it }
            this.session = session
        }

        override fun clear() {
            clearCalls += 1
            session = null
        }
    }

    private companion object {
        val validSession = AuthenticatedSession(
            accessToken = jwtWithPayload("""{"sub":"test-user-id","exp":4102444800}"""),
            tokenType = "Bearer",
            userId = "test-user-id",
            email = "ciudadano@ojociudadano.cl",
            name = "Ciudadano de prueba",
            roles = listOf("CIUDADANO")
        )

        fun jwtWithPayload(json: String): String {
            val payload = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(json.toByteArray())
            return "eyJhbGciOiJIUzI1NiJ9.$payload.sig"
        }
    }
}
