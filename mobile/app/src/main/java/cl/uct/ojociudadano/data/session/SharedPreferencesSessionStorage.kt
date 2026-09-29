package cl.uct.ojociudadano.data.session

import android.content.Context
import org.json.JSONArray

class SharedPreferencesSessionStorage(
    context: Context
) : SessionStorage {

    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    override fun currentToken(): String? {
        return currentSession()?.accessToken
    }

    override fun currentSession(): AuthenticatedSession? {
        val accessToken = preferences.getString(KEY_ACCESS_TOKEN, null)
            ?.takeIf { it.isNotBlank() }
            ?: return null

        val userId = preferences.getString(KEY_USER_ID, null) ?: return null
        val email = preferences.getString(KEY_EMAIL, null) ?: return null
        val name = preferences.getString(KEY_NAME, null) ?: return null
        val tokenType = preferences.getString(KEY_TOKEN_TYPE, null) ?: DEFAULT_TOKEN_TYPE

        return AuthenticatedSession(
            accessToken = accessToken,
            tokenType = tokenType,
            userId = userId,
            email = email,
            name = name,
            roles = preferences.getString(KEY_ROLES, null)
                ?.let(::decodeRoles)
                .orEmpty()
        )
    }

    override fun save(session: AuthenticatedSession) {
        check(
            preferences.edit()
                .putString(KEY_ACCESS_TOKEN, session.accessToken)
                .putString(KEY_TOKEN_TYPE, session.tokenType)
                .putString(KEY_USER_ID, session.userId)
                .putString(KEY_EMAIL, session.email)
                .putString(KEY_NAME, session.name)
                .putString(KEY_ROLES, JSONArray(session.roles).toString())
                .commit()
        ) { "No fue posible guardar la sesión autenticada" }
    }

    override fun clear() {
        check(preferences.edit().clear().commit()) {
            "No fue posible eliminar la sesión autenticada"
        }
    }

    private fun decodeRoles(serializedRoles: String): List<String> {
        return runCatching {
            val jsonRoles = JSONArray(serializedRoles)
            List(jsonRoles.length()) { index -> jsonRoles.getString(index) }
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val PREFERENCES_NAME = "authenticated_session"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_TOKEN_TYPE = "token_type"
        const val KEY_USER_ID = "user_id"
        const val KEY_EMAIL = "email"
        const val KEY_NAME = "name"
        const val KEY_ROLES = "roles"
        const val DEFAULT_TOKEN_TYPE = "Bearer"
    }
}
