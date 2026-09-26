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

    private fun createRepository(): RemoteIncidentRepository {
        val baseUrl = System.getenv("API_BASE_URL")
            ?: "http://localhost:3000/"

        val apiService = RetrofitClient.createApiService(baseUrl)

        return RemoteIncidentRepository(apiService)
    }
}
