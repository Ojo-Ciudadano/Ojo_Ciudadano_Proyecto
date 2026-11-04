package cl.uct.ojociudadano.data.mapper

import cl.uct.ojociudadano.data.remote.dto.AsignacionDto
import cl.uct.ojociudadano.data.remote.dto.HistorialEstadoDto
import cl.uct.ojociudadano.data.remote.dto.IncidentStatusDto
import cl.uct.ojociudadano.data.remote.dto.PaginatedResponseDto
import cl.uct.ojociudadano.data.remote.dto.PaginationMetaDto
import cl.uct.ojociudadano.data.remote.dto.UsuarioDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminIncidentMapperTest {

    @Test
    fun `historial dto maps to domain model`() {
        val dto = HistorialEstadoDto(
            id = "hist-123",
            incidenteId = "incident-123",
            estado = IncidentStatusDto("est-2", "En revisión", 2),
            usuarioId = "admin-user",
            observacion = "Se requiere visita técnica",
            fecha = "2026-11-02T10:00:00Z"
        )

        val domain = dto.toDomain()

        assertEquals("hist-123", domain.id)
        assertEquals("incident-123", domain.incidenteId)
        assertEquals("est-2", domain.status.id)
        assertEquals("En revisión", domain.status.name)
        assertEquals(2, domain.status.order)
        assertEquals("admin-user", domain.userId)
        assertEquals("Se requiere visita técnica", domain.observation)
        assertEquals("2026-11-02T10:00:00Z", domain.date)
    }

    @Test
    fun `asignacion dto maps to domain model`() {
        val dto = AsignacionDto(
            id = "asig-123",
            incidenteId = "incident-123",
            usuarioAsignadoId = "worker-1",
            asignadoPorId = "admin-1",
            fechaAsignacion = "2026-11-02T11:00:00Z",
            activa = true
        )

        val domain = dto.toDomain()

        assertEquals("asig-123", domain.id)
        assertEquals("incident-123", domain.incidenteId)
        assertEquals("worker-1", domain.assignedUserId)
        assertEquals("admin-1", domain.assignedByUserId)
        assertEquals("2026-11-02T11:00:00Z", domain.assignmentDate)
        assertTrue(domain.isActive)
    }

    @Test
    fun `usuario dto maps to candidate domain model`() {
        val dto = UsuarioDto(
            id = "user-1",
            nombre = "Ana Admin",
            email = "ana@example.invalid",
            telefono = "+56911111111",
            fechaRegistro = "2026-10-01T00:00:00Z",
            roles = listOf("ADMIN")
        )

        val candidate = dto.toCandidate()

        assertEquals("user-1", candidate.id)
        assertEquals("Ana Admin", candidate.name)
        assertEquals("ana@example.invalid", candidate.email)
        assertEquals(listOf("ADMIN"), candidate.roles)
    }

    @Test
    fun `paginated usuario response maps to paginated candidates`() {
        val response = PaginatedResponseDto(
            data = listOf(
                UsuarioDto(
                    id = "user-1",
                    nombre = "Ana Admin",
                    email = "ana@example.invalid",
                    roles = listOf("ADMIN")
                )
            ),
            meta = PaginationMetaDto(page = 2, limit = 10, total = 21, totalPages = 3)
        )

        val page = response.toCandidatePage()

        assertEquals(1, page.items.size)
        assertEquals("user-1", page.items.first().id)
        assertEquals(2, page.page)
        assertEquals(10, page.limit)
        assertEquals(21, page.total)
        assertEquals(3, page.totalPages)
    }
}
