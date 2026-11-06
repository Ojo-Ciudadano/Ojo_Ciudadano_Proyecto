package cl.uct.ojociudadano.data.session

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import cl.uct.ojociudadano.data.remote.RetrofitClient
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists one encrypted session payload. The AES key is generated and kept by Android Keystore;
 * the SharedPreferences file contains only ciphertext and its IV, never credentials or a JWT in
 * plaintext.
 */
class EncryptedSharedPreferencesSessionStorage(
    context: Context,
    environmentBaseUrl: String = RetrofitClient.DEFAULT_BASE_URL
) : SessionStorage {

    private val normalizedEnvironmentBaseUrl = RetrofitClient.normalizeBaseUrl(environmentBaseUrl)

    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    @Synchronized
    override fun currentToken(): String? = currentSession()?.accessToken

    @Synchronized
    override fun currentSession(): AuthenticatedSession? {
        val encryptedPayload = preferences.getString(KEY_ENCRYPTED_SESSION, null)

        if (encryptedPayload.isNullOrBlank()) {
            // Removes plaintext records produced by the pre-encryption implementation as well as
            // partial writes. This preferences file is dedicated exclusively to the session.
            if (preferences.all.isNotEmpty()) {
                clearSafely()
            }
            return null
        }

        val storedSession = runCatching {
            encryptedPayload.decryptStoredSession()
        }.getOrElse {
            discardInvalidSession()
            return null
        }

        if (
            storedSession.environmentBaseUrl != normalizedEnvironmentBaseUrl ||
            !storedSession.session.isLocallyValid()
        ) {
            discardInvalidSession()
            return null
        }

        return storedSession.session
    }

    @Synchronized
    override fun save(session: AuthenticatedSession) {
        require(session.isLocallyValid()) {
            "No se puede guardar una sesión autenticada inválida"
        }

        val encryptedPayload = StoredSession(
            environmentBaseUrl = normalizedEnvironmentBaseUrl,
            session = session
        ).encrypt()

        val saved = preferences.edit()
            .clear()
            .putString(KEY_ENCRYPTED_SESSION, encryptedPayload)
            .commit()

        if (!saved) {
            clearSafely()
            throw IllegalStateException("No fue posible guardar la sesión autenticada")
        }
    }

    @Synchronized
    @Suppress("UseKtx") // A synchronous result is required before exposing a new session state.
    override fun clear() {
        check(preferences.edit().clear().commit()) {
            "No fue posible eliminar la sesión autenticada"
        }
    }

    private fun clearSafely() {
        runCatching { clear() }
    }

    private fun discardInvalidSession() {
        clearSafely()
        runCatching {
            KeyStore.getInstance(ANDROID_KEY_STORE).apply {
                load(null)
                if (containsAlias(KEY_ALIAS)) {
                    deleteEntry(KEY_ALIAS)
                }
            }
        }
    }

    private fun StoredSession.encrypt(): String {
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val encrypted = cipher.doFinal(toJson().toByteArray(StandardCharsets.UTF_8))

        return listOf(
            PAYLOAD_VERSION,
            Base64.encodeToString(cipher.iv, Base64.NO_WRAP),
            Base64.encodeToString(encrypted, Base64.NO_WRAP)
        ).joinToString(separator = PAYLOAD_SEPARATOR)
    }

    private fun String.decryptStoredSession(): StoredSession {
        val parts = split(PAYLOAD_SEPARATOR, limit = PAYLOAD_PARTS)
        require(parts.size == PAYLOAD_PARTS && parts[0] == PAYLOAD_VERSION) {
            "El formato de la sesión cifrada no es válido"
        }

        val initializationVector = Base64.decode(parts[1], Base64.NO_WRAP)
        val encrypted = Base64.decode(parts[2], Base64.NO_WRAP)
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateSecretKey(),
            javax.crypto.spec.GCMParameterSpec(GCM_TAG_LENGTH_BITS, initializationVector)
        )

        return StoredSession.fromJson(
            String(cipher.doFinal(encrypted), StandardCharsets.UTF_8)
        )
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        return KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEY_STORE
        ).apply {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(AES_KEY_SIZE_BITS)
                    .build()
            )
        }.generateKey()
    }

    private data class StoredSession(
        val environmentBaseUrl: String,
        val session: AuthenticatedSession
    ) {

        fun toJson(): String = JSONObject()
            .put(JSON_SCHEMA_VERSION, SCHEMA_VERSION)
            .put(JSON_ENVIRONMENT_BASE_URL, environmentBaseUrl)
            .put(JSON_ACCESS_TOKEN, session.accessToken)
            .put(JSON_TOKEN_TYPE, session.tokenType)
            .put(JSON_USER_ID, session.userId)
            .put(JSON_EMAIL, session.email)
            .put(JSON_NAME, session.name)
            .put(JSON_ROLES, JSONArray(session.roles))
            .toString()

        companion object {
            fun fromJson(serialized: String): StoredSession {
                val json = JSONObject(serialized)
                require(json.getInt(JSON_SCHEMA_VERSION) == SCHEMA_VERSION) {
                    "La versión de sesión no es compatible"
                }

                return StoredSession(
                    environmentBaseUrl = json.requiredString(JSON_ENVIRONMENT_BASE_URL),
                    session = AuthenticatedSession(
                        accessToken = json.requiredString(JSON_ACCESS_TOKEN),
                        tokenType = json.requiredString(JSON_TOKEN_TYPE),
                        userId = json.requiredString(JSON_USER_ID),
                        email = json.requiredString(JSON_EMAIL),
                        name = json.requiredString(JSON_NAME),
                        // Older sessions without role information remain explicitly Missing.
                        // Preserve schema v1 and the original backend values for known/unknown roles.
                        roles = if (json.has(JSON_ROLES)) {
                            json.requiredStringList(JSON_ROLES)
                        } else {
                            emptyList()
                        }
                    )
                )
            }

            private fun JSONObject.requiredString(key: String): String {
                return get(key).let { value ->
                    require(value is String && value.isNotBlank()) {
                        "El campo de sesión $key no es válido"
                    }
                    value
                }
            }

            private fun JSONObject.requiredStringList(key: String): List<String> {
                val values = get(key) as? JSONArray
                    ?: throw IllegalArgumentException("El campo de sesión $key no es válido")

                return List(values.length()) { index ->
                    val value = values.get(index)
                    require(value is String && value.isNotBlank()) {
                        "El rol de sesión no es válido"
                    }
                    value
                }
            }
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "authenticated_session"
        const val KEY_ENCRYPTED_SESSION = "encrypted_session"
        const val KEY_ALIAS = "ojociudadano.authenticated_session.v1"
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        const val AES_KEY_SIZE_BITS = 256
        const val GCM_TAG_LENGTH_BITS = 128
        const val PAYLOAD_VERSION = "v1"
        const val PAYLOAD_SEPARATOR = "."
        const val PAYLOAD_PARTS = 3
        const val SCHEMA_VERSION = 1
        const val JSON_SCHEMA_VERSION = "schemaVersion"
        const val JSON_ENVIRONMENT_BASE_URL = "environmentBaseUrl"
        const val JSON_ACCESS_TOKEN = "accessToken"
        const val JSON_TOKEN_TYPE = "tokenType"
        const val JSON_USER_ID = "userId"
        const val JSON_EMAIL = "email"
        const val JSON_NAME = "name"
        const val JSON_ROLES = "roles"
    }
}
