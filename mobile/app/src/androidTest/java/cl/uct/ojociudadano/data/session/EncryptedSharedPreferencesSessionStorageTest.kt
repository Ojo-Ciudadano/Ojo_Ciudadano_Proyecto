package cl.uct.ojociudadano.data.session

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.Base64
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EncryptedSharedPreferencesSessionStorageTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val storage = EncryptedSharedPreferencesSessionStorage(context)

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
