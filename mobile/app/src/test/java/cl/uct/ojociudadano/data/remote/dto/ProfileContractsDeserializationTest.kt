package cl.uct.ojociudadano.data.remote.dto

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Contrato de `GET /api/v1/usuarios/me` (#118) tal como lo emite el backend:
 * `id`, `nombre`, `email`, `telefono` anulable, `fechaRegistro` y `roles`.
 */
class ProfileContractsDeserializationTest {

    private val gson = Gson()

    @Test
    fun `me response deserializes every profile field`() {
        val json = """
            {
              "id": "a0000000-0000-4000-8000-000000000001",
              "nombre": "Ana Ciudadana",
              "email": "ana@example.invalid",
              "telefono": "+56911111111",
              "fechaRegistro": "2026-10-01T00:00:00.000Z",
              "roles": ["CIUDADANO"]
            }
        """.trimIndent()

        val dto = gson.fromJson(json, UsuarioDto::class.java)

        assertEquals("a0000000-0000-4000-8000-000000000001", dto.id)
        assertEquals("Ana Ciudadana", dto.nombre)
        assertEquals("ana@example.invalid", dto.email)
        assertEquals("+56911111111", dto.telefono)
        assertEquals("2026-10-01T00:00:00.000Z", dto.fechaRegistro)
        assertEquals(listOf("CIUDADANO"), dto.roles)
    }

    @Test
    fun `me response tolerates null phone and multiple roles`() {
        val json = """
            {
              "id": "a0000000-0000-4000-8000-000000000002",
              "nombre": "Administrador",
              "email": "admin@example.invalid",
              "telefono": null,
              "fechaRegistro": "2026-09-15T10:30:00.000Z",
              "roles": ["ADMIN", "CIUDADANO"]
            }
        """.trimIndent()

        val dto = gson.fromJson(json, UsuarioDto::class.java)

        assertNull(dto.telefono)
        assertEquals(listOf("ADMIN", "CIUDADANO"), dto.roles)
    }
}
