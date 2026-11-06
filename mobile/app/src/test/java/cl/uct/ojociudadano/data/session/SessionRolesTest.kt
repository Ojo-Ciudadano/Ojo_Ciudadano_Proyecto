package cl.uct.ojociudadano.data.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionRolesTest {
    @Test
    fun `backend roles map exactly to supported roles`() {
        assertEquals(UserRole.CIUDADANO, UserRole.fromBackend("CIUDADANO"))
        assertEquals(UserRole.ADMIN, UserRole.fromBackend("ADMIN"))
        listOf("ADMINISTRADOR", "admin", " ADMIN", "", "INVITADO").forEach {
            assertNull(UserRole.fromBackend(it))
            assertEquals(SessionRoles.Unknown(listOf(it)), SessionRoles.fromBackend(listOf(it)))
        }
    }

    @Test
    fun `absent and empty roles are explicitly missing`() {
        assertEquals(SessionRoles.Missing, SessionRoles.fromBackend(null))
        assertEquals(SessionRoles.Missing, SessionRoles.fromBackend(emptyList()))
    }

    @Test
    fun `multiple supported roles preserve both without choosing a default`() {
        assertEquals(
            SessionRoles.Known(setOf(UserRole.ADMIN, UserRole.CIUDADANO)),
            SessionRoles.fromBackend(listOf("ADMIN", "CIUDADANO", "ADMIN"))
        )
    }

    @Test
    fun `mixed known and unknown roles remain explicitly unknown`() {
        val values = listOf("ADMIN", "INVITADO")
        assertEquals(SessionRoles.Unknown(values), SessionRoles.fromBackend(values))
    }
}
