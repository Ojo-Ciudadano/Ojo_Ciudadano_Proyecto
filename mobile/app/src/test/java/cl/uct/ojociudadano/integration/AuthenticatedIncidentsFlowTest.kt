package cl.uct.ojociudadano.integration

import cl.uct.ojociudadano.data.model.CreateIncidentInput
import cl.uct.ojociudadano.data.mapper.toRequestDto
import cl.uct.ojociudadano.data.model.Sprint1TemporaryLocation
import cl.uct.ojociudadano.data.remote.RetrofitClient
import cl.uct.ojociudadano.data.remote.error.ApiException
import cl.uct.ojociudadano.data.repository.RemoteAuthRepository
import cl.uct.ojociudadano.data.repository.RemoteIncidentRepository
import cl.uct.ojociudadano.data.session.AuthenticatedSession
import cl.uct.ojociudadano.data.session.DefaultSessionManager
import cl.uct.ojociudadano.data.session.SessionRequiredException
import cl.uct.ojociudadano.data.session.SessionState
import cl.uct.ojociudadano.data.session.TestSessionStorage
import cl.uct.ojociudadano.data.session.testSession
import com.google.gson.JsonParser
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AuthenticatedIncidentsFlowTest {
    private val server = MockWebServer()
    private val storage = TestSessionStorage()
    private var now = 1_000L
    private val manager = DefaultSessionManager(storage, Dispatchers.Unconfined) { now }
    private val userA = testSession()
    private val userB = testSession("b0000000-0000-4000-8000-000000000002")
    private val input = CreateIncidentInput(
        title = "Poste caído", description = "Bloquea la calle",
        categoryId = "c0000000-0000-4000-8000-000000000003",
        location = Sprint1TemporaryLocation.incidentLocation
    )

    @Before fun start() { server.start() }
    @After fun stop() { server.shutdown() }

    private fun api(sessionManager: DefaultSessionManager = manager) = RetrofitClient.createApiService(
        server.url("/").toString(), sessionManager, sessionManager::invalidateIfCurrent
    )

    private fun repository(sessionManager: DefaultSessionManager = manager) =
        RemoteIncidentRepository(api(sessionManager), sessionManager)

    @Test
    fun `login identity is used in query and creation with the same JWT`() = runTest {
        server.enqueue(MockResponse().setBody("""{
            "access_token":"${userA.accessToken}","token_type":"Bearer",
            "usuario":{"id":"${userA.userId}","nombre":"Test user",
            "email":"test@example.invalid","roles":["CIUDADANO"]}
        }"""))
        val auth = RemoteAuthRepository(RetrofitClient.createApiService(server.url("/").toString()))
        manager.establish(auth.login("test@example.invalid", "test-password"))
        assertNull(takeRequest().getHeader("Authorization"))

        assertUserOperations(repository(), userA)
    }

    @Test
    fun `restored session supplies identity without a new login`() = runTest {
        manager.establish(userB)
        val restored = DefaultSessionManager(storage, Dispatchers.Unconfined) { now }
        restored.restore()

        assertUserOperations(repository(restored), userB)
    }

    @Test
    fun `restoring and absent sessions block all incident requests locally`() = runTest {
        assertAllOperationsBlocked(repository())
        manager.restore()
        assertAllOperationsBlocked(repository())
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `missing or malformed identity never becomes a default reporter`() = runTest {
        for (userId in listOf("", " ", "0", "1", "not-a-uuid", "1-1-1-1-1")) {
            storage.session = userA.copy(userId = userId)
            manager.restore()
            assertAllOperationsBlocked(repository())
        }
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `expired active session is invalidated and cannot send incidents`() = runTest {
        manager.establish(testSession(expiresAt = 1_001))
        now = 1_001

        assertAllOperationsBlocked(repository())

        assertEquals(SessionState.Unauthenticated, manager.state.value)
        assertNull(storage.session)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `logout blocks old identity even if clearing persistence fails`() = runTest {
        manager.establish(userA)
        val repository = repository()
        assertUserOperations(repository, userA)
        val sentBeforeLogout = server.requestCount
        storage.failClear = true
        assertTrue(runCatching { manager.logout() }.isFailure)
        assertEquals(userA, storage.session)

        assertAllOperationsBlocked(repository)

        assertEquals(SessionState.Unauthenticated, manager.state.value)
        assertEquals(sentBeforeLogout, server.requestCount)
    }

    @Test
    fun `same repository switches to new user after logout and login`() = runTest {
        val repository = repository()
        manager.establish(userA)
        assertUserOperations(repository, userA)
        manager.logout()
        assertAllOperationsBlocked(repository)
        manager.establish(userB)

        assertUserOperations(repository, userB)
    }

    @Test
    fun `queued requests cannot mix old identity with new JWT or session generation`() = runTest {
        manager.establish(userA)
        val oldToken = manager.currentTokenSnapshot()!!
        val api = api()
        manager.establish(userB)

        assertSessionFailure { api.getIncidents(userA.userId, oldToken) }
        assertSessionFailure { api.createIncident(input.toRequestDto(userA.userId), oldToken) }
        // Even a new login returning the same JWT must reject queued old work.
        manager.establish(userA)
        assertSessionFailure { api.getIncidentById("incident-id", oldToken) }
        manager.logout()
        assertSessionFailure { api.getIncidents(userA.userId, oldToken) }

        assertEquals(0, server.requestCount)
    }

    @Test
    fun `late response from previous session is not returned to new user`() = runTest {
        val release = CountDownLatch(1)
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                check(release.await(5, TimeUnit.SECONDS))
                return MockResponse().setBody("[${incidentJson(userA)}]")
            }
        }
        manager.establish(userA)
        val pending = async(Dispatchers.IO) {
            runCatching { repository().getCurrentUserIncidents() }.exceptionOrNull()
        }
        try {
            assertEquals(userA.userId, takeRequest().requestUrl?.queryParameter("usuarioReportaId"))
            manager.establish(userB)
        } finally {
            release.countDown()
        }

        assertTrue(pending.await() is SessionRequiredException)
        assertEquals(SessionState.Authenticated(userB), manager.state.value)
    }

    @Test
    fun `401 preserves existing invalidation and prevents subsequent identity reuse`() = runTest {
        manager.establish(userA)
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"message":"Token inválido"}"""))

        val failure = runCatching { repository().getCurrentUserIncidents() }.exceptionOrNull()

        assertTrue(failure is ApiException.Http)
        assertEquals(401, (failure as ApiException.Http).statusCode)
        assertEquals("Bearer ${userA.accessToken}", takeRequest().getHeader("Authorization"))
        assertEquals(SessionState.Unauthenticated, manager.state.value)
        assertAllOperationsBlocked(repository())
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `general incident query remains unfiltered`() = runTest {
        manager.establish(userA)
        server.enqueue(MockResponse().setBody("[]"))

        assertTrue(repository().getIncidents().isEmpty())

        val request = takeRequest()
        assertEquals("/api/v1/incidentes", request.path)
        assertEquals("Bearer ${userA.accessToken}", request.getHeader("Authorization"))
    }

    private suspend fun assertUserOperations(repository: RemoteIncidentRepository, user: AuthenticatedSession) {
        server.enqueue(MockResponse().setBody("[${incidentJson(user)}]"))
        server.enqueue(MockResponse().setResponseCode(201).setBody(incidentJson(user)))

        assertEquals(user.userId, repository.getCurrentUserIncidents().single().reporterUserId)
        assertEquals(user.userId, repository.createIncident(input).reporterUserId)

        val query = takeRequest()
        val create = takeRequest()
        assertEquals("GET", query.method)
        assertEquals(user.userId, query.requestUrl?.queryParameter("usuarioReportaId"))
        assertEquals(setOf("usuarioReportaId"), query.requestUrl?.queryParameterNames)
        assertEquals("POST", create.method)
        assertEquals("/api/v1/incidentes", create.path)
        for (request in listOf(query, create)) {
            assertEquals("Bearer ${user.accessToken}", request.getHeader("Authorization"))
        }
        val json = JsonParser.parseString(create.body.readUtf8()).asJsonObject
        assertEquals(user.userId, json["usuarioReportaId"].asString)
        assertEquals(setOf("titulo", "descripcion", "categoriaId", "usuarioReportaId", "ubicacion"), json.keySet())
    }

    private suspend fun assertAllOperationsBlocked(repository: RemoteIncidentRepository) {
        assertSessionFailure { repository.getCurrentUserIncidents() }
        assertSessionFailure { repository.createIncident(input) }
        assertSessionFailure { repository.getIncidents() }
        assertSessionFailure { repository.getIncidentById("incident-id") }
    }

    private suspend fun assertSessionFailure(block: suspend () -> Any) {
        val failure = runCatching { block() }.exceptionOrNull()
        assertTrue("Expected controlled session failure, got $failure", failure is SessionRequiredException)
    }

    private fun takeRequest() = checkNotNull(server.takeRequest(5, TimeUnit.SECONDS))

    private fun incidentJson(user: AuthenticatedSession) = """{
        "id":"d0000000-0000-4000-8000-000000000004","titulo":"Poste caído",
        "descripcion":"Bloquea la calle","categoriaId":"${input.categoryId}",
        "estadoId":"estado","ubicacionId":"ubicacion","usuarioReportaId":"${user.userId}",
        "fechaCreacion":"2026-10-26T00:00:00Z","fechaActualizacion":"2026-10-26T00:00:00Z",
        "categoria":{"id":"${input.categoryId}","nombre":"Alumbrado","descripcion":"",
        "prioridadBase":1,"esEmergenciaCritica":false},
        "estado":{"id":"estado","nombre":"Nuevo","orden":1},
        "ubicacion":{"id":"ubicacion","latitud":-38.7359,"longitud":-72.5904,
        "direccionReferencia":"Plaza","comuna":"Temuco","region":"Araucanía","sector":"Centro"}
    }"""
}
