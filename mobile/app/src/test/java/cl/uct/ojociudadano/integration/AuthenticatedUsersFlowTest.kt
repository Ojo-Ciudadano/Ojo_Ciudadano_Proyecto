package cl.uct.ojociudadano.integration

import cl.uct.ojociudadano.data.remote.RetrofitClient
import cl.uct.ojociudadano.data.remote.error.ApiException
import cl.uct.ojociudadano.data.repository.RemoteIncidentRepository
import cl.uct.ojociudadano.data.session.DefaultSessionManager
import cl.uct.ojociudadano.data.session.TestSessionStorage
import cl.uct.ojociudadano.data.session.UserRole
import cl.uct.ojociudadano.data.session.testSession
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test

class AuthenticatedUsersFlowTest {
    @Test fun `users reuse bearer auth omit all role and map server metadata and current roles`() = runTest {
        val server = MockWebServer()
        server.start()
        try {
            val session = testSession().copy(roles = listOf("ADMIN"))
            val manager = DefaultSessionManager(TestSessionStorage(session)).also { it.restore() }
            val api = RetrofitClient.createApiService(server.url("/").toString(), tokenProvider = manager,
                onUnauthorized = manager::invalidateIfCurrent)
            val repository = RemoteIncidentRepository(api, manager)
            for (role in listOf(null, UserRole.CIUDADANO, UserRole.ADMIN)) {
                server.enqueue(MockResponse().setBody("""{
                    "data":[
                      {"id":"u1","nombre":"Ana","email":"ana@example.invalid","roles":["ADMIN"]},
                      {"id":"u2","nombre":"Luis","email":"luis@example.invalid","roles":["CIUDADANO"]}
                    ],
                    "meta":{"page":2,"limit":10,"total":23,"totalPages":3}
                }"""))
                val result = repository.getUsers(2, 10, role)
                val request = server.takeRequest()
                assertEquals("/api/v1/usuarios", request.requestUrl!!.encodedPath)
                assertEquals("2", request.requestUrl!!.queryParameter("page"))
                assertEquals("10", request.requestUrl!!.queryParameter("limit"))
                assertEquals(role?.backendValue, request.requestUrl!!.queryParameter("rol"))
                assertEquals("Bearer ${session.accessToken}", request.getHeader("Authorization"))
                assertEquals(2, result.page)
                assertEquals(10, result.limit)
                assertEquals(23, result.total)
                assertEquals(3, result.totalPages)
                assertEquals(listOf("Ana", "Luis"), result.items.map { it.name })
                assertEquals(listOf("ana@example.invalid", "luis@example.invalid"), result.items.map { it.email })
                assertEquals(listOf(listOf("ADMIN"), listOf("CIUDADANO")), result.items.map { it.roles })
            }
            manager.establish(testSession())
            try {
                repository.getUsers(1, 10, null)
                fail("Citizen query must fail before networking")
            } catch (error: ApiException.Http) { assertEquals(403, error.statusCode) }
            assertEquals(3, server.requestCount)
        } finally { server.shutdown() }
    }
}
