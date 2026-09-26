package cl.uct.ojociudadano.data.repository

import cl.uct.ojociudadano.data.model.CreateIncidentInput
import cl.uct.ojociudadano.data.model.CreateIncidentLocationInput
import cl.uct.ojociudadano.data.remote.ApiService
import cl.uct.ojociudadano.data.remote.dto.CategoryDto
import cl.uct.ojociudadano.data.remote.dto.CreateIncidentRequestDto
import cl.uct.ojociudadano.data.remote.dto.IncidentDto
import cl.uct.ojociudadano.data.remote.dto.IncidentLocationDto
import cl.uct.ojociudadano.data.remote.dto.IncidentStatusDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RemoteRepositoryTest {

    @Test
    fun `getIncidents without user forwards null filter and maps response`() = runTest {
        val apiService = FakeApiService(
            incidents = listOf(sampleIncidentDto())
        )
        val repository = RemoteIncidentRepository(apiService)

        val incidents = repository.getIncidents()

        assertNull(apiService.lastReporterUserId)
        assertEquals(1, incidents.size)
        assertEquals("Poste caído", incidents.single().title)
        assertEquals("Alumbrado público", incidents.single().category.name)
        assertEquals("Nuevo", incidents.single().status.name)
        assertEquals("Temuco", incidents.single().location.commune)
    }

    @Test
    fun `getIncidentsByReporter forwards reporter user id`() = runTest {
        val apiService = FakeApiService()
        val repository = RemoteIncidentRepository(apiService)
        val userId = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"

        repository.getIncidentsByReporter(userId)

        assertEquals(userId, apiService.lastReporterUserId)
    }

    @Test
    fun `getIncidentById forwards id and maps response`() = runTest {
        val apiService = FakeApiService(
            incident = sampleIncidentDto()
        )
        val repository = RemoteIncidentRepository(apiService)

        val incident = repository.getIncidentById(
            "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
        )

        assertEquals(
            "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
            apiService.lastIncidentId
        )
        assertEquals("Poste caído", incident.title)
        assertEquals(-38.7359, incident.location.latitude, 0.0)
    }

    @Test
    fun `createIncident maps input to request and response to domain`() = runTest {
        val apiService = FakeApiService(
            incident = sampleIncidentDto()
        )
        val repository = RemoteIncidentRepository(apiService)

        val input = CreateIncidentInput(
            title = "Poste caído",
            description = "Poste bloqueando parcialmente la calle",
            categoryId = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
            reporterUserId = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
            location = CreateIncidentLocationInput(
                latitude = -38.7359,
                longitude = -72.5904,
                referenceAddress = "Frente a la plaza",
                commune = "Temuco",
                region = "La Araucanía",
                sector = "Centro"
            )
        )

        val incident = repository.createIncident(input)

        val request = requireNotNull(apiService.lastCreateRequest)

        assertEquals("Poste caído", request.titulo)
        assertEquals(
            "Poste bloqueando parcialmente la calle",
            request.descripcion
        )
        assertEquals(
            "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
            request.categoriaId
        )
        assertEquals(
            "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
            request.usuarioReportaId
        )

        assertEquals(-38.7359, request.ubicacion.latitud, 0.0)
        assertEquals(-72.5904, request.ubicacion.longitud, 0.0)
        assertEquals("Frente a la plaza", request.ubicacion.direccionReferencia)
        assertEquals("Temuco", request.ubicacion.comuna)
        assertEquals("La Araucanía", request.ubicacion.region)
        assertEquals("Centro", request.ubicacion.sector)

        assertEquals("Poste caído", incident.title)
        assertEquals("Nuevo", incident.status.name)
    }

    @Test
    fun `getCategories maps API response to domain models`() = runTest {
        val apiService = FakeApiService(
            categories = listOf(
                sampleCategoryDto()
            )
        )
        val repository = RemoteCategoryRepository(apiService)

        val categories = repository.getCategories()

        assertEquals(1, categories.size)

        val category = categories.single()

        assertEquals(
            "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
            category.id
        )
        assertEquals("Alumbrado público", category.name)
        assertEquals(2, category.basePriority)
        assertEquals(false, category.isCriticalEmergency)
        assertNull(category.officialContactChannel)
    }

    private class FakeApiService(
        private val incidents: List<IncidentDto> = emptyList(),
        private val incident: IncidentDto = sampleIncidentDto(),
        private val categories: List<CategoryDto> = emptyList()
    ) : ApiService {

        var lastReporterUserId: String? = null
            private set

        var lastIncidentId: String? = null
            private set

        var lastCreateRequest: CreateIncidentRequestDto? = null
            private set

        override suspend fun getIncidents(
            usuarioReportaId: String?
        ): List<IncidentDto> {
            lastReporterUserId = usuarioReportaId
            return incidents
        }

        override suspend fun getIncidentById(
            id: String
        ): IncidentDto {
            lastIncidentId = id
            return incident
        }

        override suspend fun getCategories(): List<CategoryDto> {
            return categories
        }

        override suspend fun createIncident(
            request: CreateIncidentRequestDto
        ): IncidentDto {
            lastCreateRequest = request
            return incident
        }
    }

    companion object {

        private fun sampleCategoryDto(): CategoryDto {
            return CategoryDto(
                id = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                nombre = "Alumbrado público",
                descripcion = "Fallas de alumbrado",
                prioridadBase = 2,
                esEmergenciaCritica = false,
                canalOficialContacto = null
            )
        }

        private fun sampleIncidentDto(): IncidentDto {
            return IncidentDto(
                id = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                titulo = "Poste caído",
                descripcion = "Poste bloqueando parcialmente la calle",
                categoriaId = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                estadoId = "cccccccc-cccc-cccc-cccc-cccccccccccc",
                ubicacionId = "dddddddd-dddd-dddd-dddd-dddddddddddd",
                usuarioReportaId = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
                nivelPrioridad = null,
                fechaCreacion = "2026-09-24T01:00:00.000Z",
                fechaActualizacion = "2026-09-24T01:00:00.000Z",
                categoria = sampleCategoryDto(),
                estado = IncidentStatusDto(
                    id = "cccccccc-cccc-cccc-cccc-cccccccccccc",
                    nombre = "Nuevo",
                    orden = 1
                ),
                ubicacion = IncidentLocationDto(
                    id = "dddddddd-dddd-dddd-dddd-dddddddddddd",
                    latitud = -38.7359,
                    longitud = -72.5904,
                    direccionReferencia = "Frente a la plaza",
                    comuna = "Temuco",
                    region = "La Araucanía",
                    sector = "Centro"
                )
            )
        }
    }
}
