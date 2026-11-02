package cl.uct.ojociudadano.data.mapper

import cl.uct.ojociudadano.data.remote.dto.AsignacionDto
import cl.uct.ojociudadano.data.remote.dto.HistorialEstadoDto
import cl.uct.ojociudadano.data.remote.dto.IncidentStatusDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminIncidentMapperTest {

    @Test
    fun `historial dto maps to domain model`() {
        val dto = HistorialEstadoDto(
            id = "hist-123",
            estado = IncidentStatusDto("est-2", "En revisión", 2),
            usuarioId = "admin-user",
            observacion = "Se requiere visita técnica",
            fecha = "2026-11-02T10:00:00Z"
        )

        val domain = dto.toDomain()

        assertEquals("hist-123", domain.id)
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
            usuarioAsignadoId = "worker-1",
            asignadoPorId = "admin-1",
            fechaAsignacion = "2026-11-02T11:00:00Z",
            activa = true
        )

        val domain = dto.toDomain()

        assertEquals("asig-123", domain.id)
        assertEquals("worker-1", domain.assignedUserId)
        assertEquals("admin-1", domain.assignedByUserId)
        assertEquals("2026-11-02T11:00:00Z", domain.assignmentDate)
        assertTrue(domain.isActive)
    }
}
