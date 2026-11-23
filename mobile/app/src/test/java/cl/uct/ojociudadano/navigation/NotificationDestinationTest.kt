package cl.uct.ojociudadano.navigation

import cl.uct.ojociudadano.data.session.AuthenticatedSession
import cl.uct.ojociudadano.data.session.SessionState
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationDestinationTest {

    private val incidentId = "d0000000-0000-4000-8000-000000000004"

    @Test
    fun `blank identifiers do not produce a destination`() {
        assertNull(notificationIncidentRoute(citizenSession(), "   "))
        assertNull(notificationIncidentRoute(citizenSession(), null))
    }

    @Test
    fun `identifiers that would break the route are rejected`() {
        assertNull(notificationIncidentRoute(citizenSession(), "incidente/otro"))
        assertNull(notificationIncidentRoute(citizenSession(), "incidente?x=1"))
        assertNull(notificationIncidentRoute(citizenSession(), "incidente#fragmento"))
    }

    @Test
    fun `citizen session opens the citizen detail`() {
        assertEquals(
            "incident/$incidentId",
            notificationIncidentRoute(citizenSession(), incidentId)
        )
    }

    @Test
    fun `administrator session opens the administrative detail`() {
        assertEquals(
            "admin/incidents/$incidentId",
            notificationIncidentRoute(adminSession(), incidentId)
        )
    }

    @Test
    fun `identifier validity is shared by route and click outcome`() {
        assertTrue(isValidIncidentId(incidentId))
        assertTrue(isValidIncidentId("  $incidentId  "))
        assertFalse(isValidIncidentId(null))
        assertFalse(isValidIncidentId("   "))
        assertFalse(isValidIncidentId("incidente/otro"))
        assertFalse(isValidIncidentId("incidente?x=1"))
        assertFalse(isValidIncidentId("incidente#fragmento"))
    }

    @Test
    fun `click outcome distinguishes open, invalid and missing incidents`() {
        assertEquals(
            NotificationClickOutcome.Open(incidentId),
            notificationClickOutcome(incidentId)
        )
        assertEquals(
            NotificationClickOutcome.Open(incidentId),
            notificationClickOutcome("  $incidentId  ")
        )
        assertEquals(
            NotificationClickOutcome.InvalidIncident,
            notificationClickOutcome("incidente/otro")
        )
        assertEquals(NotificationClickOutcome.NoIncident, notificationClickOutcome("  "))
        assertEquals(NotificationClickOutcome.NoIncident, notificationClickOutcome(null))
    }

    @Test
    fun `identifier is trimmed before building the route`() {
        assertEquals(
            "incident/$incidentId",
            notificationIncidentRoute(citizenSession(), "  $incidentId  ")
        )
    }

    private fun citizenSession() = session("CIUDADANO")

    private fun adminSession() = session("ADMIN")

    private fun session(role: String) = SessionState.Authenticated(
        AuthenticatedSession(
            accessToken = token(),
            tokenType = "Bearer",
            userId = "a0000000-0000-4000-8000-000000000001",
            email = "test@example.invalid",
            name = "Test user",
            roles = listOf(role)
        )
    )

    private fun token(): String {
        val payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
            """{"sub":"a0000000-0000-4000-8000-000000000001","exp":4102444800}""".toByteArray()
        )
        return "eyJhbGciOiJIUzI1NiJ9.$payload.test-signature"
    }
}
