package cl.uct.ojociudadano.data.session

import java.io.IOException
import java.util.UUID

/** Identity for one operation, read from the same session that supplies its JWT. */
data class SessionIdentity(val userId: String, val token: TokenSnapshot)

class SessionRequiredException : IOException(
    "La sesión no está disponible o cambió. Inicia sesión nuevamente."
)

fun SessionManager.requireIdentity(): SessionIdentity {
    // This also delegates expiration/invalidation to the existing session manager.
    val token = currentTokenSnapshot() ?: throw SessionRequiredException()
    val session = (state.value as? SessionState.Authenticated)?.session
        ?: throw SessionRequiredException()
    val validUserId = runCatching {
        UUID.fromString(session.userId).toString().equals(session.userId, ignoreCase = true)
    }.getOrDefault(false)

    if (!validUserId || session.accessToken != token.value || currentTokenSnapshot() != token) {
        throw SessionRequiredException()
    }
    return SessionIdentity(session.userId, token)
}
