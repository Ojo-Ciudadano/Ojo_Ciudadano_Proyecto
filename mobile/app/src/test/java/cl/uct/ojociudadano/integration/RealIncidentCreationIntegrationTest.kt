package cl.uct.ojociudadano.integration

import cl.uct.ojociudadano.data.model.CreateIncidentInput
import cl.uct.ojociudadano.data.model.Sprint1TemporaryLocation
import cl.uct.ojociudadano.data.remote.RetrofitClient
import cl.uct.ojociudadano.data.remote.Sprint1ApiDefaults
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

        // 2. Nuevo SessionStorage falso en memoria para la prueba
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

        // 3. Crear ApiService CON el sessionStorage y hacer login
        val apiService = RetrofitClient.createApiService(baseUrl, sessionStorage)
        sessionStorage.save(
            cl.uct.ojociudadano.data.repository.RemoteAuthRepository(apiService)
                .login(email, password)
        )

        val categoryRepository = RemoteCategoryRepository(apiService)
        val incidentRepository = RemoteIncidentRepository(apiService)

        // --- A partir de aquí, el resto del test queda exactamente igual ---
        val category = categoryRepository
            .getCategories()
            .first { it.name == "Alumbrado público" }

        val marker = UUID.randomUUID().toString()

        val input = CreateIncidentInput(
            title = "Prueba integración móvil $marker",
            description = "Incidente creado para validar el POST real desde Android",
            categoryId = category.id,
            reporterUserId = Sprint1ApiDefaults.USER_ID,
            location = Sprint1TemporaryLocation.incidentLocation
        )

        val created = incidentRepository.createIncident(input)

        assertNotNull(UUID.fromString(created.id))
        assertEquals(input.title, created.title)
        assertEquals(input.description, created.description)
        assertEquals(Sprint1ApiDefaults.USER_ID, created.reporterUserId)

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
        assertEquals(Sprint1ApiDefaults.USER_ID, persisted.reporterUserId)

        val userIncidents = incidentRepository.getIncidentsByReporter(
            Sprint1ApiDefaults.USER_ID
        )

        assertTrue(
            userIncidents.any { it.id == created.id }
        )
    }
}
