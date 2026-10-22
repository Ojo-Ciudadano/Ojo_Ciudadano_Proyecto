package cl.uct.ojociudadano.integration

import cl.uct.ojociudadano.data.remote.RetrofitClient
import cl.uct.ojociudadano.data.remote.Sprint1ApiDefaults
import cl.uct.ojociudadano.data.repository.RemoteIncidentRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class RealIncidentRepositoryIntegrationTest {

    @Test
    fun `getIncidents obtains general incidents through API Gateway`() = runBlocking {
        assumeTrue(
            "La prueba requiere RUN_API_INTEGRATION_TESTS=true",
            System.getenv("RUN_API_INTEGRATION_TESTS") == "true"
        )

        val repository = createRepository()

        val incidents = repository.getIncidents()

        assertTrue(incidents.isNotEmpty())

        assertTrue(
            incidents.any {
                it.title == "Poste sin iluminación" &&
                        it.reporterUserId == Sprint1ApiDefaults.USER_ID &&
                        it.category.name == "Alumbrado público" &&
                        it.status.name == "Nuevo" &&
                        it.location.commune == "Temuco"
            }
        )

        assertTrue(
            incidents.any {
                it.title == "Basura acumulada" &&
                        it.reporterUserId != Sprint1ApiDefaults.USER_ID
            }
        )
    }

    @Test
    fun `getIncidents filters incidents by Sprint 1 user`() = runBlocking {
        assumeTrue(
            "La prueba requiere RUN_API_INTEGRATION_TESTS=true",
            System.getenv("RUN_API_INTEGRATION_TESTS") == "true"
        )

        val repository = createRepository()

        val incidents = repository.getIncidentsByReporter(
            Sprint1ApiDefaults.USER_ID
        )

        assertTrue(incidents.isNotEmpty())

        assertTrue(
            incidents.all {
                it.reporterUserId == Sprint1ApiDefaults.USER_ID
            }
        )

        assertTrue(
            incidents.any {
                it.title == "Poste sin iluminación"
            }
        )
    }

    @Test
    fun `filtered incidents are contained in general incident query`() = runBlocking {
        assumeTrue(
            "La prueba requiere RUN_API_INTEGRATION_TESTS=true",
            System.getenv("RUN_API_INTEGRATION_TESTS") == "true"
        )

        val repository = createRepository()

        val allIncidents = repository.getIncidents()
        val userIncidents = repository.getIncidentsByReporter(
            Sprint1ApiDefaults.USER_ID
        )

        val allIds = allIncidents.map { it.id }.toSet()

        assertTrue(userIncidents.isNotEmpty())
        assertTrue(userIncidents.all { it.id in allIds })

        assertEquals(
            Sprint1ApiDefaults.USER_ID,
            userIncidents.first().reporterUserId
        )
    }

    private suspend fun createRepository(): RemoteIncidentRepository {
        val email = System.getenv("AUTH_EMAIL").orEmpty()
        val password = System.getenv("AUTH_PASSWORD").orEmpty()

        assumeTrue(
            "La prueba requiere AUTH_EMAIL y AUTH_PASSWORD",
            email.isNotBlank() && password.isNotBlank()
        )

        val baseUrl = System.getenv("API_BASE_URL")
            ?: RetrofitClient.DEFAULT_BASE_URL

        val sessionStorage = object : cl.uct.ojociudadano.data.session.SessionStorage {
            private var session: cl.uct.ojociudadano.data.session.AuthenticatedSession? = null
            override fun currentToken(): String? = session?.accessToken
            override fun currentSession() = session
            override fun save(session: cl.uct.ojociudadano.data.session.AuthenticatedSession) {
                this.session = session
            }

            override fun clear() {
                session = null
            }
        }

        val apiService = RetrofitClient.createApiService(baseUrl, sessionStorage)
        sessionStorage.save(
            cl.uct.ojociudadano.data.repository.RemoteAuthRepository(apiService)
                .login(email, password)
        )

        return RemoteIncidentRepository(apiService)
    }
}
