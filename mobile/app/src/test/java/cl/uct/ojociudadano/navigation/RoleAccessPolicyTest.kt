package cl.uct.ojociudadano.navigation

import cl.uct.ojociudadano.data.session.DefaultSessionManager
import cl.uct.ojociudadano.data.session.SessionState
import cl.uct.ojociudadano.data.session.TestSessionStorage
import cl.uct.ojociudadano.data.session.testSession
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoleAccessPolicyTest {
    private val citizenRoutes = listOf(AppRoutes.HOME, AppRoutes.REPORTS,
        AppRoutes.INCIDENT_DETAIL, "incident/test-id", "incident/id%2Fencoded",
        AppRoutes.CREATE_REPORT, AppRoutes.NOTIFICATIONS, AppRoutes.MAP, AppRoutes.PROFILE)
    private val routes = citizenRoutes + listOf(AppRoutes.ADMIN, AppRoutes.ROLE_UNAVAILABLE,
        AppRoutes.LOGIN, AppRoutes.REGISTER)

    @Test fun `each valid role can access only its own screens and actions`() {
        for (route in routes) {
            assertEquals(route, route in citizenRoutes, RoleAccessPolicy.allows(state("CIUDADANO"), route))
            assertEquals(route, route == AppRoutes.ADMIN, RoleAccessPolicy.allows(state("ADMIN"), route))
        }
    }

    @Test fun `restoration grants nothing and anonymous users only access authentication`() {
        for (route in routes) {
            assertFalse(RoleAccessPolicy.allows(SessionState.Restoring, route))
            assertEquals(route, route in listOf(AppRoutes.LOGIN, AppRoutes.REGISTER),
                RoleAccessPolicy.allows(SessionState.Unauthenticated, route))
        }
        assertNull(RoleAccessPolicy.safeRoute(SessionState.Restoring))
    }

    @Test fun `unknown missing ambiguous expired and inconsistent sessions grant no role privileges`() {
        val invalid = listOf(state(), state("INVITADO"), state("ADMIN", "INVITADO"),
            state("ADMIN", "CIUDADANO"), state("CIUDADANO", "ADMIN"), state("admin"),
            SessionState.Authenticated(testSession(expiresAt = 1)),
            SessionState.Authenticated(testSession().copy(userId = "")))
        for (state in invalid) {
            for (route in routes) {
                assertEquals(route, route == AppRoutes.ROLE_UNAVAILABLE, RoleAccessPolicy.allows(state, route))
            }
            assertEquals(AppRoutes.ROLE_UNAVAILABLE, RoleAccessPolicy.safeRoute(state))
        }
    }

    @Test fun `unknown and malformed routes are denied by default`() {
        for (route in listOf(null, "", "future_admin", "admin/anything", "incident/", "incident/a/b",
            "incident/a?other=value", "incident/a#other")) {
            assertFalse(RoleAccessPolicy.allows(state("CIUDADANO"), route))
            assertFalse(RoleAccessPolicy.allows(state("ADMIN"), route))
        }
    }

    @Test fun `every fallback is authorized and stable without a redirect loop`() {
        for (state in listOf(state("CIUDADANO"), state("ADMIN"), state(), SessionState.Unauthenticated)) {
            val safe = RoleAccessPolicy.safeRoute(state)
            assertTrue(RoleAccessPolicy.allows(state, safe))
            assertEquals(safe, RoleAccessPolicy.safeRoute(state))
        }
    }

    @Test fun `permissions follow restoration logout and both account change directions`() = runTest {
        val manager = DefaultSessionManager(TestSessionStorage(testSession().copy(roles = listOf("ADMIN"))))
        assertFalse(RoleAccessPolicy.allows(manager.state.value, AppRoutes.ADMIN))
        manager.restore()
        for (role in listOf("ADMIN", "CIUDADANO", "ADMIN")) {
            manager.establish(testSession().copy(roles = listOf(role)))
            assertEquals(role == "ADMIN", RoleAccessPolicy.allows(manager.state.value, AppRoutes.ADMIN))
            assertEquals(role == "CIUDADANO", RoleAccessPolicy.allows(manager.state.value, AppRoutes.CREATE_REPORT))
            manager.logout()
            assertFalse(RoleAccessPolicy.allows(manager.state.value, AppRoutes.ADMIN))
            assertFalse(RoleAccessPolicy.allows(manager.state.value, AppRoutes.CREATE_REPORT))
        }
    }

    private fun state(vararg roles: String) =
        SessionState.Authenticated(testSession().copy(roles = roles.toList()))
}
