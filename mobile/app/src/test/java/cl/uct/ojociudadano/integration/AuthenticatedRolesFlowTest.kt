package cl.uct.ojociudadano.integration

import cl.uct.ojociudadano.data.remote.RetrofitClient
import cl.uct.ojociudadano.data.repository.RemoteAuthRepository
import cl.uct.ojociudadano.data.session.AuthenticatedSession
import cl.uct.ojociudadano.data.session.DefaultSessionManager
import cl.uct.ojociudadano.data.session.SessionRoles
import cl.uct.ojociudadano.data.session.SessionState
import cl.uct.ojociudadano.data.session.SessionStorage
import cl.uct.ojociudadano.data.session.UserRole
import java.util.Base64
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/** Exercises JSON deserialization, repository mapping and the observable session together. */
class AuthenticatedRolesFlowTest {
    private val server = MockWebServer()
    private val storage = MemoryStorage()
    private val manager = DefaultSessionManager(storage)
    private lateinit var repository: RemoteAuthRepository

    @Before
    fun setUp() {
        server.start()
        repository = RemoteAuthRepository(
            RetrofitClient.createApiService(server.url("/").toString())
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `citizen login uses backend role despite administrator display name`() = runTest {
        login("citizen", """["CIUDADANO"]""")
        assertRoleSurvivesRestart(SessionRoles.Known(setOf(UserRole.CIUDADANO)), "citizen")
    }

    @Test
    fun `administrator login stores and restores backend role`() = runTest {
        login("admin", """["ADMIN"]""")
        assertRoleSurvivesRestart(SessionRoles.Known(setOf(UserRole.ADMIN)), "admin")
    }

    @Test
    fun `unknown role remains unknown through persistence and restart`() = runTest {
        login("unknown", """["ADMINISTRADOR"]""")
        assertRoleSurvivesRestart(SessionRoles.Unknown(listOf("ADMINISTRADOR")), "unknown")
    }

    @Test
    fun `missing null and empty response roles never default to citizen`() = runTest {
        for (roles in listOf(null, "null", "[]")) {
            login("missing", roles)
            assertRoleSurvivesRestart(SessionRoles.Missing, "missing")
        }
    }

    @Test
    fun `changing identity replaces all prior administrator roles`() = runTest {
        login("admin", """["ADMIN"]""")
        login("citizen", """["CIUDADANO"]""")
        assertRoleSurvivesRestart(SessionRoles.Known(setOf(UserRole.CIUDADANO)), "citizen")
        login("missing", null)
        assertRoleSurvivesRestart(SessionRoles.Missing, "missing")
        login("unknown", """["INVITADO"]""")
        assertRoleSurvivesRestart(SessionRoles.Unknown(listOf("INVITADO")), "unknown")
    }

    @Test
    fun `logout removes role and identity from memory and restored state`() = runTest {
        login("admin", """["ADMIN"]""")
        manager.logout()
        assertEquals(SessionState.Unauthenticated, manager.state.value)
        assertNull(storage.currentSession())
        assertNull(manager.currentToken())
        assertEquals(SessionState.Unauthenticated, DefaultSessionManager(storage).restore())
    }

    @Test
    fun `delayed unauthorized request cannot clear roles of a newer login`() = runTest {
        login("admin", """["ADMIN"]""")
        val oldToken = requireNotNull(manager.currentTokenSnapshot())
        login("citizen", """["CIUDADANO"]""")
        manager.invalidateIfCurrent(oldToken)
        assertRoleSurvivesRestart(SessionRoles.Known(setOf(UserRole.CIUDADANO)), "citizen")
        manager.invalidateIfCurrent(requireNotNull(manager.currentTokenSnapshot()))
        assertEquals(SessionState.Unauthenticated, manager.state.value)
        assertNull(storage.currentSession())
    }

    private suspend fun login(userId: String, rolesJson: String?) {
        val payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
            """{"sub":"$userId","exp":4102444800}""".toByteArray()
        )
        val rolesField = rolesJson?.let { """, "roles": $it""" }.orEmpty()
        server.enqueue(MockResponse().setBody("""
            {"access_token":"header.$payload.signature", "token_type":"Bearer",
             "usuario":{"id":"$userId", "nombre":"Administrador",
                        "email":"$userId@example.invalid"$rolesField}}
        """.trimIndent()))
        manager.establish(repository.login("$userId@example.invalid", "test-password"))
        val request = server.takeRequest()
        assertEquals("/api/v1/auth/login", request.path)
        assertNull(request.getHeader("Authorization"))
    }

    private suspend fun assertRoleSurvivesRestart(expected: SessionRoles, userId: String) {
        val current = (manager.state.value as SessionState.Authenticated).session
        assertEquals(userId, current.userId)
        assertEquals(expected, current.roleState)
        assertEquals(expected, storage.currentSession()?.roleState)
        val restored = DefaultSessionManager(storage).restore() as SessionState.Authenticated
        assertEquals(current, restored.session)
        assertEquals(expected, restored.session.roleState)
    }

    private class MemoryStorage : SessionStorage {
        private var session: AuthenticatedSession? = null
        override fun currentToken(): String? = session?.accessToken
        override fun currentSession(): AuthenticatedSession? = session
        override fun save(session: AuthenticatedSession) { this.session = session }
        override fun clear() { session = null }
    }
}
