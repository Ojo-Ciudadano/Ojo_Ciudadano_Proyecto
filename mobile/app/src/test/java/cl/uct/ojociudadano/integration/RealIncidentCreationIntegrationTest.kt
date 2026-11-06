package cl.uct.ojociudadano.integration

import cl.uct.ojociudadano.data.model.CreateIncidentInput
import cl.uct.ojociudadano.data.model.Sprint1TemporaryLocation
import cl.uct.ojociudadano.data.remote.RetrofitClient
import cl.uct.ojociudadano.data.session.DefaultSessionManager
import cl.uct.ojociudadano.data.session.TestSessionStorage
import cl.uct.ojociudadano.data.repository.RemoteAuthRepository
import cl.uct.ojociudadano.data.repository.RemoteCategoryRepository
import cl.uct.ojociudadano.data.repository.RemoteIncidentRepository
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class RealIncidentCreationIntegrationTest {

    @Test
    fun `createIncident persists incident through API Gateway`() = runBlocking {
        assumeTrue(
            "La prueba de escritura requiere RUN_API_INTEGRATION_TESTS=true y RUN_WRITE_INTEGRATION_TESTS=true",
            System.getenv("RUN_API_INTEGRATION_TESTS") == "true" &&
                    System.getenv("RUN_WRITE_INTEGRATION_TESTS") == "true"
        )

        val email = System.getenv("AUTH_EMAIL").orEmpty()
        val password = System.getenv("AUTH_PASSWORD").orEmpty()

        assumeTrue(
            "La prueba requiere AUTH_EMAIL y AUTH_PASSWORD",
            email.isNotBlank() && password.isNotBlank()
        )

        val baseUrl = System.getenv("API_BASE_URL")
            ?: RetrofitClient.DEFAULT_BASE_URL

        val session = RemoteAuthRepository(RetrofitClient.createApiService(baseUrl))
            .login(email, password)
        val manager = DefaultSessionManager(TestSessionStorage())
        manager.establish(session)
        val apiService = RetrofitClient.createApiService(baseUrl, manager, manager::invalidateIfCurrent)
        val categoryRepository = RemoteCategoryRepository(apiService)
        val incidentRepository = RemoteIncidentRepository(apiService, manager)

        val category = categoryRepository
            .getCategories()
            .first { it.name == "Alumbrado público" }

        val marker = UUID.randomUUID().toString()

        val input = CreateIncidentInput(
            title = "Prueba integración móvil $marker",
            description = "Incidente creado para validar el POST real desde Android",
            categoryId = category.id,
            location = Sprint1TemporaryLocation.incidentLocation
        )

        val created = incidentRepository.createIncident(input)

        assertNotNull(UUID.fromString(created.id))
        assertEquals(input.title, created.title)
        assertEquals(input.description, created.description)
        assertEquals(session.userId, created.reporterUserId)

        assertEquals(category.id, created.category.id)
        assertEquals("Alumbrado público", created.category.name)

        assertEquals("Nuevo", created.status.name)
        assertEquals(1, created.status.order)

        assertEquals(input.location.latitude, created.location.latitude, 0.0)
        assertEquals(input.location.longitude, created.location.longitude, 0.0)
        assertEquals(
            input.location.referenceAddress,
            created.location.referenceAddress
        )
        assertEquals(input.location.commune, created.location.commune)
        assertEquals(input.location.region, created.location.region)
        assertEquals(input.location.sector, created.location.sector)

        val persisted = incidentRepository.getIncidentById(created.id)

        assertEquals(created.id, persisted.id)
        assertEquals(input.title, persisted.title)
        assertEquals(session.userId, persisted.reporterUserId)

        val userIncidents = incidentRepository.getCurrentUserIncidents()

        assertTrue(
            userIncidents.any { it.id == created.id }
        )
    }
}
