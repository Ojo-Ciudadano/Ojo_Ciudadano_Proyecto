package cl.uct.ojociudadano.data.remote.dto

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class ResponseDtoDeserializationTest {

    private val gson = Gson()

    @Test
    fun `login response deserializes token and user from usuario`() {
        val json = """
            {
              "access_token": "jwt-value",
              "token_type": "Bearer",
              "usuario": {
                "id": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
                "nombre": "Administrador",
                "email": "admin@ojociudadano.cl",
                "roles": ["CIUDADANO"]
              },
              "user": {
                "id": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
                "nombre": "Administrador",
                "email": "admin@ojociudadano.cl",
                "roles": ["CIUDADANO"]
              }
            }
        """.trimIndent()

        val response = gson.fromJson(json, LoginResponseDto::class.java)

        assertEquals("jwt-value", response.access_token)
        assertEquals("Bearer", response.token_type)
        assertEquals("Administrador", response.authenticatedUser().nombre)
        assertEquals(listOf("CIUDADANO"), response.authenticatedUser().roles)
    }

    @Test
    fun `incident response deserializes according to backend contract`() {
        val json = """
            {
              "id": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
              "titulo": "Poste caído",
              "descripcion": "Poste bloqueando parcialmente la calle",
              "categoriaId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
              "estadoId": "cccccccc-cccc-cccc-cccc-cccccccccccc",
              "ubicacionId": "dddddddd-dddd-dddd-dddd-dddddddddddd",
              "usuarioReportaId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
              "nivelPrioridad": null,
              "fechaCreacion": "2026-09-23T20:00:00.000Z",
              "fechaActualizacion": "2026-09-23T20:00:00.000Z",
              "categoria": {
                "id": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                "nombre": "Alumbrado público",
                "descripcion": "Fallas de alumbrado en calles y espacios públicos",
                "prioridadBase": 2,
                "esEmergenciaCritica": false,
                "canalOficialContacto": null
              },
              "estado": {
                "id": "cccccccc-cccc-cccc-cccc-cccccccccccc",
                "nombre": "Nuevo",
                "orden": 1
              },
              "ubicacion": {
                "id": "dddddddd-dddd-dddd-dddd-dddddddddddd",
                "latitud": -38.7359,
                "longitud": -72.5904,
                "direccionReferencia": "Frente a la plaza",
                "comuna": "Temuco",
                "region": "La Araucanía",
                "sector": "Centro"
              }
            }
        """.trimIndent()

        val incident = gson.fromJson(json, IncidentDto::class.java)

        assertEquals("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", incident.id)
        assertEquals("Poste caído", incident.titulo)
        assertEquals(
            "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
            incident.usuarioReportaId
        )
        assertNull(incident.nivelPrioridad)

        assertEquals("Alumbrado público", incident.categoria.nombre)
        assertEquals(2, incident.categoria.prioridadBase)
        assertFalse(incident.categoria.esEmergenciaCritica)
        assertNull(incident.categoria.canalOficialContacto)

        assertEquals("Nuevo", incident.estado.nombre)
        assertEquals(1, incident.estado.orden)

        assertEquals(-38.7359, incident.ubicacion.latitud, 0.0)
        assertEquals(-72.5904, incident.ubicacion.longitud, 0.0)
        assertEquals("Temuco", incident.ubicacion.comuna)
        assertEquals("La Araucanía", incident.ubicacion.region)
    }

    @Test
    fun `categories response deserializes according to backend contract`() {
        val json = """
            [
              {
                "id": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                "nombre": "Alumbrado público",
                "descripcion": "Fallas de alumbrado en calles y espacios públicos",
                "prioridadBase": 2,
                "esEmergenciaCritica": false,
                "canalOficialContacto": null
              },
              {
                "id": "eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee",
                "nombre": "Incendio",
                "descripcion": "Incendios estructurales, forestales o pastizales",
                "prioridadBase": 5,
                "esEmergenciaCritica": true,
                "canalOficialContacto": "Bomberos 132"
              }
            ]
        """.trimIndent()

        val type = object : TypeToken<List<CategoryDto>>() {}.type
        val categories: List<CategoryDto> = gson.fromJson(json, type)

        assertEquals(2, categories.size)

        assertEquals("Alumbrado público", categories[0].nombre)
        assertFalse(categories[0].esEmergenciaCritica)
        assertNull(categories[0].canalOficialContacto)

        assertEquals("Incendio", categories[1].nombre)
        assertEquals(5, categories[1].prioridadBase)
        assertEquals("Bomberos 132", categories[1].canalOficialContacto)
    }
}
