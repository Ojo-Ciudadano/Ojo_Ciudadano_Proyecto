package cl.uct.ojociudadano.data.remote.dto

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contratos JSON de la capa administrativa tal como los emite el backend:
 * `{ data, meta }` para usuarios, y asignación/historial/detalle de incidente.
 */
class AdminContractsDeserializationTest {

    private val gson = Gson()

    @Test
    fun `paginated usuarios response deserializes data and meta`() {
        val json = """
            {
              "data": [
                {
                  "id": "a0000000-0000-4000-8000-000000000001",
                  "nombre": "Ana Admin",
                  "email": "ana@example.invalid",
                  "telefono": "+56911111111",
                  "fechaRegistro": "2026-10-01T00:00:00.000Z",
                  "roles": ["ADMIN"]
                }
              ],
              "meta": { "page": 1, "limit": 10, "total": 1, "totalPages": 1 }
            }
        """.trimIndent()

        val type = object : TypeToken<PaginatedResponseDto<UsuarioDto>>() {}.type
        val response: PaginatedResponseDto<UsuarioDto> = gson.fromJson(json, type)

        assertEquals(1, response.data.size)
        assertEquals("Ana Admin", response.data.first().nombre)
        assertEquals("+56911111111", response.data.first().telefono)
        assertEquals(listOf("ADMIN"), response.data.first().roles)
        assertEquals(1, response.meta.page)
        assertEquals(10, response.meta.limit)
        assertEquals(1, response.meta.total)
        assertEquals(1, response.meta.totalPages)
    }

    @Test
    fun `usuario without optional fields deserializes with nulls`() {
        val json = """
            {
              "id": "a0000000-0000-4000-8000-000000000002",
              "nombre": "Ciudadano",
              "email": "ciudadano@example.invalid",
              "roles": ["CIUDADANO"]
            }
        """.trimIndent()

        val usuario = gson.fromJson(json, UsuarioDto::class.java)

        assertNull(usuario.telefono)
        assertNull(usuario.fechaRegistro)
    }

    @Test
    fun `asignacion response deserializes according to backend contract`() {
        val json = """
            {
              "id": "asig-123",
              "incidenteId": "incident-123",
              "usuarioAsignadoId": "worker-1",
              "asignadoPorId": "admin-1",
              "fechaAsignacion": "2026-11-02T11:00:00.000Z",
              "activa": true
            }
        """.trimIndent()

        val asignacion = gson.fromJson(json, AsignacionDto::class.java)

        assertEquals("asig-123", asignacion.id)
        assertEquals("incident-123", asignacion.incidenteId)
        assertEquals("worker-1", asignacion.usuarioAsignadoId)
        assertEquals("admin-1", asignacion.asignadoPorId)
        assertEquals("2026-11-02T11:00:00.000Z", asignacion.fechaAsignacion)
        assertTrue(asignacion.activa)
    }

    @Test
    fun `historial response deserializes nested estado and incidenteId`() {
        val json = """
            [
              {
                "id": "hist-1",
                "incidenteId": "incident-123",
                "estado": { "id": "est-2", "nombre": "En revisión", "orden": 2 },
                "usuarioId": "admin-user",
                "observacion": "Se requiere visita técnica",
                "fecha": "2026-11-02T10:00:00.000Z"
              },
              {
                "id": "hist-2",
                "incidenteId": "incident-123",
                "estado": { "id": "est-1", "nombre": "Nuevo", "orden": 1 },
                "usuarioId": "citizen-user",
                "observacion": null,
                "fecha": "2026-11-01T09:00:00.000Z"
              }
            ]
        """.trimIndent()

        val type = object : TypeToken<List<HistorialEstadoDto>>() {}.type
        val historial: List<HistorialEstadoDto> = gson.fromJson(json, type)

        assertEquals(2, historial.size)
        assertEquals("incident-123", historial[0].incidenteId)
        assertEquals("En revisión", historial[0].estado.nombre)
        assertEquals(2, historial[0].estado.orden)
        assertEquals("Se requiere visita técnica", historial[0].observacion)
        assertNull(historial[1].observacion)
    }

    @Test
    fun `incident detail deserializes historial and asignacionActiva`() {
        val json = """
            {
              "id": "incident-123",
              "titulo": "Poste caído",
              "descripcion": "Poste bloqueando parcialmente la calle",
              "categoriaId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
              "estadoId": "cccccccc-cccc-cccc-cccc-cccccccccccc",
              "ubicacionId": "dddddddd-dddd-dddd-dddd-dddddddddddd",
              "usuarioReportaId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
              "nivelPrioridad": 2,
              "fechaCreacion": "2026-11-01T09:00:00.000Z",
              "fechaActualizacion": "2026-11-02T11:00:00.000Z",
              "categoria": {
                "id": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                "nombre": "Alumbrado público",
                "descripcion": "Fallas de alumbrado",
                "prioridadBase": 2,
                "esEmergenciaCritica": false,
                "canalOficialContacto": null
              },
              "estado": { "id": "cccccccc-cccc-cccc-cccc-cccccccccccc", "nombre": "En revisión", "orden": 2 },
              "ubicacion": {
                "id": "dddddddd-dddd-dddd-dddd-dddddddddddd",
                "latitud": -38.7359,
                "longitud": -72.5904,
                "direccionReferencia": "Frente a la plaza",
                "comuna": "Temuco",
                "region": "La Araucanía",
                "sector": "Centro"
              },
              "historial": [
                {
                  "id": "hist-1",
                  "incidenteId": "incident-123",
                  "estado": { "id": "cccccccc-cccc-cccc-cccc-cccccccccccc", "nombre": "En revisión", "orden": 2 },
                  "usuarioId": "admin-user",
                  "observacion": "Asignado",
                  "fecha": "2026-11-02T10:00:00.000Z"
                }
              ],
              "asignacionActiva": {
                "id": "asig-123",
                "incidenteId": "incident-123",
                "usuarioAsignadoId": "worker-1",
                "asignadoPorId": "admin-1",
                "fechaAsignacion": "2026-11-02T11:00:00.000Z",
                "activa": true
              }
            }
        """.trimIndent()

        val incident = gson.fromJson(json, IncidentDto::class.java)

        assertEquals(2, incident.nivelPrioridad)
        assertEquals(1, incident.historial?.size)
        assertEquals("hist-1", incident.historial?.first()?.id)
        assertEquals("incident-123", incident.asignacionActiva?.incidenteId)
        assertEquals("worker-1", incident.asignacionActiva?.usuarioAsignadoId)
    }
}
