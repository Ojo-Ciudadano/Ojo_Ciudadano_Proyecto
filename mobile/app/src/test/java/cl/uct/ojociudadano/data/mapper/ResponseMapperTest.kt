package cl.uct.ojociudadano.data.mapper

import cl.uct.ojociudadano.data.remote.dto.CategoryDto
import cl.uct.ojociudadano.data.remote.dto.EvidenceDto
import cl.uct.ojociudadano.data.remote.dto.IncidentDto
import cl.uct.ojociudadano.data.remote.dto.IncidentLocationDto
import cl.uct.ojociudadano.data.remote.dto.IncidentStatusDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResponseMapperTest {

    @Test
    fun `category dto maps to domain model`() {
        val dto = CategoryDto(
            id = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
            nombre = "Alumbrado público",
            descripcion = "Fallas de alumbrado",
            prioridadBase = 2,
            esEmergenciaCritica = false,
            canalOficialContacto = null
        )

        val category = dto.toDomain()

        assertEquals(dto.id, category.id)
        assertEquals("Alumbrado público", category.name)
        assertEquals("Fallas de alumbrado", category.description)
        assertEquals(2, category.basePriority)
        assertFalse(category.isCriticalEmergency)
        assertNull(category.officialContactChannel)
    }

    @Test
    fun `incident dto maps complete response to domain model`() {
        val dto = sampleIncidentDto(evidencias = listOf(sampleEvidenceDto()))

        val incident = dto.toDomain()

        assertEquals(dto.id, incident.id)
        assertEquals("Poste caído", incident.title)
        assertEquals(
            "Poste bloqueando parcialmente la calle",
            incident.description
        )

        assertEquals("Alumbrado público", incident.category.name)
        assertEquals(2, incident.category.basePriority)

        assertEquals("Nuevo", incident.status.name)
        assertEquals(1, incident.status.order)

        assertEquals(-38.7359, incident.location.latitude, 0.0)
        assertEquals(-72.5904, incident.location.longitude, 0.0)
        assertEquals("Frente a la plaza", incident.location.referenceAddress)
        assertEquals("Temuco", incident.location.commune)

        assertEquals(
            "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
            incident.reporterUserId
        )
        assertNull(incident.priorityLevel)
        assertEquals("2026-09-23T20:00:00.000Z", incident.createdAt)
        assertEquals("2026-09-23T20:05:00.000Z", incident.updatedAt)

        assertEquals(1, incident.evidences.size)
        assertEquals("evidencia-1", incident.evidences.single().id)
        assertEquals(incident.id, incident.evidences.single().incidentId)
        assertEquals("image/jpeg", incident.evidences.single().mimeType)
        assertEquals(2048L, incident.evidences.single().size)
        assertEquals("2026-09-23T20:03:00.000Z", incident.evidences.single().createdAt)
        assertEquals("/evidencias/evidencia-1/archivo", incident.evidences.single().fileUrl)
    }

    @Test
    fun `incident dto maps every evidence preserving order`() {
        val dto = sampleIncidentDto(
            evidencias = listOf(
                sampleEvidenceDto(),
                sampleEvidenceDto().copy(
                    id = "evidencia-2",
                    tipoMime = "image/png",
                    url = "/evidencias/evidencia-2/archivo"
                )
            )
        )

        val evidences = dto.toDomain().evidences

        assertEquals(listOf("evidencia-1", "evidencia-2"), evidences.map { it.id })
        assertEquals(listOf("image/jpeg", "image/png"), evidences.map { it.mimeType })
    }

    @Test
    fun `incident dto maps missing or empty evidencias to an empty list`() {
        val missing = sampleIncidentDto(evidencias = null).toDomain()
        val empty = sampleIncidentDto(evidencias = emptyList()).toDomain()

        assertTrue(missing.evidences.isEmpty())
        assertTrue(empty.evidences.isEmpty())
    }

    @Test
    fun `evidence dto maps to domain model`() {
        val dto = sampleEvidenceDto()

        val evidence = dto.toDomain()

        assertEquals(dto.id, evidence.id)
        assertEquals(dto.incidenteId, evidence.incidentId)
        assertEquals(dto.tipoMime, evidence.mimeType)
        assertEquals(dto.tamanoArchivo, evidence.size)
        assertEquals(dto.fechaCreacion, evidence.createdAt)
        assertEquals(dto.url, evidence.fileUrl)
    }

    private fun sampleIncidentDto(evidencias: List<EvidenceDto>? = null): IncidentDto {
        return IncidentDto(
            id = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
            titulo = "Poste caído",
            descripcion = "Poste bloqueando parcialmente la calle",
            categoriaId = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
            estadoId = "cccccccc-cccc-cccc-cccc-cccccccccccc",
            ubicacionId = "dddddddd-dddd-dddd-dddd-dddddddddddd",
            usuarioReportaId = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
            nivelPrioridad = null,
            fechaCreacion = "2026-09-23T20:00:00.000Z",
            fechaActualizacion = "2026-09-23T20:05:00.000Z",
            categoria = CategoryDto(
                id = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                nombre = "Alumbrado público",
                descripcion = "Fallas de alumbrado",
                prioridadBase = 2,
                esEmergenciaCritica = false,
                canalOficialContacto = null
            ),
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
            ),
            evidencias = evidencias
        )
    }

    private fun sampleEvidenceDto(): EvidenceDto {
        return EvidenceDto(
            id = "evidencia-1",
            incidenteId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
            tipoMime = "image/jpeg",
            tamanoArchivo = 2048L,
            fechaCreacion = "2026-09-23T20:03:00.000Z",
            url = "/evidencias/evidencia-1/archivo"
        )
    }
}
