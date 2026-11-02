package cl.uct.ojociudadano.data.session

import com.google.gson.JsonParser
import java.nio.charset.StandardCharsets
import java.util.Base64

data class AuthenticatedSession(
    val accessToken: String,
    val tokenType: String,
    val userId: String,
    val email: String,
    val name: String,
    val roles: List<String>
) {
    /** Derived from this user's persisted login response, never from UI labels or JWT decoding. */
    val roleState: SessionRoles
        get() = SessionRoles.fromBackend(roles)

    fun isLocallyValid(nowEpochSeconds: Long = System.currentTimeMillis() / 1000): Boolean {
        return accessToken.isNotBlank() &&
            tokenType.isNotBlank() &&
            userId.isNotBlank() &&
            email.isNotBlank() &&
            name.isNotBlank() &&
            roles.all { it.isNotBlank() } &&
            !isExpired(nowEpochSeconds)
    }

    fun isExpired(nowEpochSeconds: Long = System.currentTimeMillis() / 1000): Boolean {
        val parts = accessToken.split('.')
        if (parts.size != 3 || parts[1].isBlank()) {
            return true
        }

        val expSeconds = runCatching {
            val payloadBytes = Base64.getUrlDecoder().decode(parts[1])
            val payloadJson = String(payloadBytes, StandardCharsets.UTF_8)
            val jsonObject = JsonParser.parseString(payloadJson).asJsonObject
            jsonObject.get("exp")?.takeIf { it.isJsonPrimitive }?.asLong
        }.getOrNull() ?: return true

        return expSeconds <= nowEpochSeconds
    }
}
