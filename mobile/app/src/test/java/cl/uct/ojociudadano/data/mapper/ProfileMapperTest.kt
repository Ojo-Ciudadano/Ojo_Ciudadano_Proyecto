package cl.uct.ojociudadano.data.mapper

import cl.uct.ojociudadano.data.remote.dto.UsuarioDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileMapperTest {

    @Test
    fun `maps every field of the me contract`() {
        val dto = UsuarioDto(
            id = "a0000000-0000-4000-8000-000000000001",
            nombre = "Ana Ciudadana",
            email = "ana@example.invalid",
            telefono = "+56911111111",
            fechaRegistro = "2026-10-01T00:00:00.000Z",
            roles = listOf("CIUDADANO")
        )

        val profile = dto.toProfile()

        assertEquals(dto.id, profile.id)
        assertEquals("Ana Ciudadana", profile.name)
        assertEquals("ana@example.invalid", profile.email)
        assertEquals("+56911111111", profile.phone)
        assertEquals("2026-10-01T00:00:00.000Z", profile.registeredAt)
        assertEquals(listOf("CIUDADANO"), profile.roles)
    }

    @Test
    fun `keeps optional fields null instead of inventing values`() {
        val dto = UsuarioDto(
            id = "a0000000-0000-4000-8000-000000000002",
            nombre = "Sin Teléfono",
            email = "sintel@example.invalid",
            telefono = null,
            fechaRegistro = null,
            roles = emptyList()
        )

        val profile = dto.toProfile()

        assertNull(profile.phone)
        assertNull(profile.registeredAt)
        assertEquals(emptyList<String>(), profile.roles)
    }
}
