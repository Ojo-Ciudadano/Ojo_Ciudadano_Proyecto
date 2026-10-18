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

    fun isExpired(nowEpochSeconds: Long = System.currentTimeMillis() / 1000): Boolean {
        val parts = accessToken.split('.')
        if (parts.size != 3) {
            return false
        }

        val expSeconds = runCatching {
            val payloadBytes = Base64.getUrlDecoder().decode(parts[1])
            val payloadJson = String(payloadBytes, StandardCharsets.UTF_8)
            val jsonObject = JsonParser.parseString(payloadJson).asJsonObject
            jsonObject.get("exp")?.takeIf { it.isJsonPrimitive }?.asLong
        }.getOrNull() ?: return false

        return expSeconds <= nowEpochSeconds
    }
}
