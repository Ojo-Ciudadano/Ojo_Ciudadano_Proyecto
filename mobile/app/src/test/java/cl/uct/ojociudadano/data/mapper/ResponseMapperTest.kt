package cl.uct.ojociudadano.data.mapper

import cl.uct.ojociudadano.data.remote.dto.CategoryDto
import cl.uct.ojociudadano.data.remote.dto.IncidentDto
import cl.uct.ojociudadano.data.remote.dto.IncidentLocationDto
import cl.uct.ojociudadano.data.remote.dto.IncidentStatusDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
        val dto = IncidentDto(
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
            )
        )

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
    }
}
