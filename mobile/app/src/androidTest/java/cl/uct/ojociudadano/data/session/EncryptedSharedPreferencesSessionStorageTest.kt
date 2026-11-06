package cl.uct.ojociudadano.data.session

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.SecretKey
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Before
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EncryptedSharedPreferencesSessionStorageTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val storage = EncryptedSharedPreferencesSessionStorage(context)

    @Before
    fun setUp() {
        storage.clear()
    }

    @After
    fun tearDown() {
        storage.clear()
    }

    @Test
    fun savesOnlyEncryptedPayloadAndRestoresSession() {
        storage.save(validSession)

        val values = context.getSharedPreferences(
            "authenticated_session",
            Context.MODE_PRIVATE
        ).all
        val encryptedPayload = values["encrypted_session"] as? String

        assertEquals(setOf("encrypted_session"), values.keys)
        assertTrue(encryptedPayload.orEmpty().startsWith("v1."))
        assertFalse(encryptedPayload.orEmpty().contains(validSession.accessToken))
        assertFalse(encryptedPayload.orEmpty().contains(validSession.email))
        assertFalse(encryptedPayload.orEmpty().contains("password-not-stored"))
        assertEquals(validSession, storage.currentSession())
    }

    @Test
    fun corruptPayloadIsClearedAndNotRestored() {
        context.getSharedPreferences("authenticated_session", Context.MODE_PRIVATE)
            .edit()
            .putString("encrypted_session", "not-a-valid-encrypted-payload")
            .commit()

        assertNull(storage.currentSession())
        assertTrue(
            context.getSharedPreferences("authenticated_session", Context.MODE_PRIVATE)
                .all
                .isEmpty()
        )
    }

    @Test
    fun rolesSurviveEncryptedStorageAndManagerRecreation() = runBlocking {
        val cases = listOf(
            listOf("CIUDADANO") to SessionRoles.Known(setOf(UserRole.CIUDADANO)),
            listOf("ADMIN") to SessionRoles.Known(setOf(UserRole.ADMIN)),
            listOf("ADMIN", "INVITADO") to SessionRoles.Unknown(listOf("ADMIN", "INVITADO")),
            emptyList<String>() to SessionRoles.Missing
        )
        for ((roles, expected) in cases) {
            val manager = DefaultSessionManager(storage)
            manager.establish(validSession.copy(roles = roles))
            val recreatedStorage = EncryptedSharedPreferencesSessionStorage(context)
            val restored = DefaultSessionManager(recreatedStorage).restore()
                as SessionState.Authenticated
            assertEquals(validSession.userId, restored.session.userId)
            assertEquals(roles, restored.session.roles)
            assertEquals(expected, restored.session.roleState)
        }
    }

    @Test
    fun changingAccountAndLogoutRemovePreviousEncryptedRoles() = runBlocking {
        val manager = DefaultSessionManager(storage)
        manager.establish(validSession.copy(roles = listOf("ADMIN")))
        val next = validSession.copy(userId = "next-user", roles = listOf("CIUDADANO"))
        manager.establish(next)
        val recreatedStorage = EncryptedSharedPreferencesSessionStorage(context)
        assertEquals(next, recreatedStorage.currentSession())
        manager.logout()
        assertNull(recreatedStorage.currentSession())
        assertEquals(SessionState.Unauthenticated, DefaultSessionManager(recreatedStorage).restore())
        assertTrue(context.getSharedPreferences("authenticated_session", Context.MODE_PRIVATE).all.isEmpty())
    }

    @Test
    fun legacySchemaWithRolesRestoresTypedRole() {
        writeLegacySession(JSONArray().put("ADMIN"))
        val restored = EncryptedSharedPreferencesSessionStorage(context).currentSession()
        assertEquals(validSession.userId, restored?.userId)
        assertEquals(SessionRoles.Known(setOf(UserRole.ADMIN)), restored?.roleState)
    }

    @Test
    fun legacySchemaWithoutRolesRestoresExplicitMissingState() {
        writeLegacySession(null)
        val restored = EncryptedSharedPreferencesSessionStorage(context).currentSession()
        assertEquals(validSession.userId, restored?.userId)
        assertEquals(SessionRoles.Missing, restored?.roleState)
    }

    @Test
    fun malformedStoredRolesAreDiscarded() {
        writeLegacySession("ADMIN")
        assertNull(storage.currentSession())
        assertTrue(context.getSharedPreferences("authenticated_session", Context.MODE_PRIVATE).all.isEmpty())
    }

    /** Independent schema-v1 fixture encrypted with the real Android Keystore key. */
    private fun writeLegacySession(roles: Any?) {
        storage.save(validSession) // Ensure the existing key has been generated.
        val json = JSONObject()
            .put("schemaVersion", 1)
            .put("environmentBaseUrl", "https://ojociudadano.dev.censei.cl/")
            .put("accessToken", validSession.accessToken)
            .put("tokenType", "Bearer")
            .put("userId", validSession.userId)
            .put("email", validSession.email)
            .put("name", validSession.name)
        if (roles != null) json.put("roles", roles)
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val key = keyStore.getKey("ojociudadano.authenticated_session.v1", null) as SecretKey
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, key)
        }
        val encrypted = cipher.doFinal(json.toString().toByteArray(Charsets.UTF_8))
        val encoder = Base64.getEncoder()
        val payload = "v1.${encoder.encodeToString(cipher.iv)}.${encoder.encodeToString(encrypted)}"
        assertTrue(context.getSharedPreferences("authenticated_session", Context.MODE_PRIVATE)
            .edit().clear().putString("encrypted_session", payload).commit())
    }

    private companion object {
        val validSession = AuthenticatedSession(
            accessToken = jwtWithPayload("""{"sub":"instrumented-user","exp":4102444800}"""),
            tokenType = "Bearer",
            userId = "instrumented-user",
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
