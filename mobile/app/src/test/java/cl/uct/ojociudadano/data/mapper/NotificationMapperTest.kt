package cl.uct.ojociudadano.data.mapper

import cl.uct.ojociudadano.data.remote.dto.NotificationDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class NotificationMapperTest {

    @Test
    fun `notification dto maps to domain preserving incident and date`() {
        val dto = NotificationDto(
            id = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
            usuarioId = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
            incidenteId = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
            mensaje = "Su incidente cambió de estado",
            leido = false,
            fecha = "2026-09-24T01:00:00.000Z"
        )

        val notification = dto.toDomain()

        assertEquals("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", notification.id)
        assertEquals("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11", notification.userId)
        assertEquals("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb", notification.incidentId)
        assertEquals("Su incidente cambió de estado", notification.message)
        assertFalse(notification.isRead)
        assertEquals("2026-09-24T01:00:00.000Z", notification.createdAt)
    }
}
