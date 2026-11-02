package cl.uct.ojociudadano.navigation

import cl.uct.ojociudadano.data.session.DefaultSessionManager
import cl.uct.ojociudadano.data.session.SessionState
import cl.uct.ojociudadano.data.session.TestSessionStorage
import cl.uct.ojociudadano.data.session.testSession
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InitialDestinationTest {
    @Test fun `citizen and administrator select distinct destinations`() {
        assertEquals(InitialDestination.CITIZEN, resolve("CIUDADANO"))
        assertEquals(InitialDestination.ADMINISTRATOR, resolve("ADMIN"))
    }

    @Test fun `missing session selects authentication`() {
        assertEquals(InitialDestination.AUTHENTICATION,
            resolveInitialDestination(SessionState.Unauthenticated))
    }

    @Test fun `restoration has no functional destination`() {
        val destination = resolveInitialDestination(SessionState.Restoring)
        assertEquals(InitialDestination.LOADING, destination)
        assertNull(destination.route)
    }

    @Test fun `missing unknown mixed and ambiguous roles grant neither flow`() {
        listOf(emptyList(), listOf("INVITADO"), listOf("ADMIN", "INVITADO"),
            listOf("ADMIN", "CIUDADANO"), listOf("CIUDADANO", "ADMIN"),
            listOf("admin"), listOf("")).forEach { roles ->
            assertEquals(InitialDestination.UNAVAILABLE, resolve(*roles.toTypedArray()))
        }
    }

    @Test fun `duplicate backend values are one role according to session model`() {
        assertEquals(InitialDestination.ADMINISTRATOR, resolve("ADMIN", "ADMIN"))
    }

    @Test fun `inconsistent and expired authenticated sessions cannot select a role flow`() {
        listOf(testSession().copy(userId = ""), testSession(expiresAt = 1)).forEach {
            assertEquals(InitialDestination.UNAVAILABLE,
                resolveInitialDestination(SessionState.Authenticated(it.copy(roles = listOf("ADMIN")))))
        }
    }

    @Test fun `real manager restores each role then logout returns to authentication`() = runTest {
        for ((role, expected) in listOf("CIUDADANO" to InitialDestination.CITIZEN,
            "ADMIN" to InitialDestination.ADMINISTRATOR)) {
            val storage = TestSessionStorage(testSession().copy(roles = listOf(role)))
            val manager = DefaultSessionManager(storage)
            assertEquals(InitialDestination.LOADING, resolveInitialDestination(manager.state.value))
            assertEquals(expected, resolveInitialDestination(manager.restore()))
            manager.logout()
            assertEquals(InitialDestination.AUTHENTICATION, resolveInitialDestination(manager.state.value))
            assertEquals(InitialDestination.AUTHENTICATION,
                resolveInitialDestination(DefaultSessionManager(storage).restore()))
        }
    }

    @Test fun `login and account changes replace the previous destination`() = runTest {
        val manager = DefaultSessionManager(TestSessionStorage())
        manager.restore()
        manager.establish(testSession().copy(roles = listOf("ADMIN")))
        assertEquals(InitialDestination.ADMINISTRATOR, resolveInitialDestination(manager.state.value))
        manager.establish(testSession(userId = "another-user"))
        assertEquals(InitialDestination.CITIZEN, resolveInitialDestination(manager.state.value))
        manager.establish(testSession().copy(roles = emptyList()))
        assertEquals(InitialDestination.UNAVAILABLE, resolveInitialDestination(manager.state.value))
    }

    private fun resolve(vararg roles: String) = resolveInitialDestination(
        SessionState.Authenticated(testSession().copy(roles = roles.toList()))
    )
}
