package cl.uct.ojociudadano.data.remote.dto

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class NotificationContractsDeserializationTest {

    private val gson = Gson()

    @Test
    fun `notification response deserializes according to backend contract`() {
        val json = """
            {
              "id": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
              "usuarioId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
              "incidenteId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
              "mensaje": "Su incidente cambió de estado a 'En revisión'",
              "leido": false,
              "fecha": "2026-09-24T01:00:00.000Z"
            }
        """.trimIndent()

        val notification = gson.fromJson(json, NotificationDto::class.java)

        assertEquals("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", notification.id)
        assertEquals("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11", notification.usuarioId)
        assertEquals("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb", notification.incidenteId)
        assertEquals("Su incidente cambió de estado a 'En revisión'", notification.mensaje)
        assertFalse(notification.leido)
        assertEquals("2026-09-24T01:00:00.000Z", notification.fecha)
    }

    @Test
    fun `empty notifications array deserializes to empty list`() {
        val json = "[]"

        val type = object : TypeToken<List<NotificationDto>>() {}.type
        val notifications: List<NotificationDto> = gson.fromJson(json, type)

        assertEquals(emptyList<NotificationDto>(), notifications)
    }
}
