package cl.uct.ojociudadano.data.session

import java.util.Base64
import kotlinx.coroutines.Dispatchers

class TestSessionStorage(var session: AuthenticatedSession? = null) : SessionStorage {
    var failClear = false
    override fun currentToken() = session?.accessToken
    override fun currentSession() = session
    override fun save(session: AuthenticatedSession) { this.session = session }
    override fun clear() {
        check(!failClear) { "Storage unavailable" }
        session = null
    }
}

fun testSession(
    userId: String = "a0000000-0000-4000-8000-000000000001",
    expiresAt: Long = 4102444800
): AuthenticatedSession {
    val payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
        """{"sub":"$userId","exp":$expiresAt}""".toByteArray()
    )
    return AuthenticatedSession(
        accessToken = "eyJhbGciOiJIUzI1NiJ9.$payload.test-signature",
        tokenType = "Bearer",
        userId = userId,
        email = "test@example.invalid",
        name = "Test user",
        roles = listOf("CIUDADANO")
    )
}

suspend fun testSessionManager(): DefaultSessionManager =
    DefaultSessionManager(TestSessionStorage(testSession()), Dispatchers.Unconfined).also {
        it.restore()
    }
