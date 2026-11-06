package cl.uct.ojociudadano.data.remote.dto

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class CreateIncidentRequestDtoTest {

    private val gson = Gson()

    @Test
    fun `request serializes using backend contract field names`() {
        val request = CreateIncidentRequestDto(
            titulo = "Poste caído",
            descripcion = "Poste caído bloqueando parcialmente la calle",
            categoriaId = "11111111-1111-1111-1111-111111111111",
            usuarioReportaId = "a0000000-0000-4000-8000-000000000001",
            ubicacion = LocationRequestDto(
                latitud = -38.7359,
                longitud = -72.5904,
                direccionReferencia = "Frente a la plaza",
                comuna = "Temuco",
                region = "La Araucanía",
                sector = "Centro"
            )
        )

        val json = gson.toJsonTree(request).asJsonObject

        assertEquals(
            setOf(
                "titulo",
                "descripcion",
                "categoriaId",
                "usuarioReportaId",
                "ubicacion"
            ),
            json.keySet()
        )

        assertEquals("Poste caído", json["titulo"].asString)
        assertEquals(
            "Poste caído bloqueando parcialmente la calle",
            json["descripcion"].asString
        )
        assertEquals(
            "11111111-1111-1111-1111-111111111111",
            json["categoriaId"].asString
        )
        assertEquals(
            "a0000000-0000-4000-8000-000000000001",
            json["usuarioReportaId"].asString
        )
    }

    @Test
    fun `location serializes as nested object using backend contract field names`() {
        val request = CreateIncidentRequestDto(
            titulo = "Poste caído",
            descripcion = "Descripción",
            categoriaId = "11111111-1111-1111-1111-111111111111",
            usuarioReportaId = "a0000000-0000-4000-8000-000000000001",
            ubicacion = LocationRequestDto(
                latitud = -38.7359,
                longitud = -72.5904,
                direccionReferencia = "Frente a la plaza",
                comuna = "Temuco",
                region = "La Araucanía",
                sector = "Centro"
            )
        )

        val ubicacion = gson
            .toJsonTree(request)
            .asJsonObject["ubicacion"]
            .asJsonObject

        assertEquals(
            setOf(
                "latitud",
                "longitud",
                "direccionReferencia",
                "comuna",
                "region",
                "sector"
            ),
            ubicacion.keySet()
        )

        assertEquals(-38.7359, ubicacion["latitud"].asDouble, 0.0)
        assertEquals(-72.5904, ubicacion["longitud"].asDouble, 0.0)
        assertEquals(
            "Frente a la plaza",
            ubicacion["direccionReferencia"].asString
        )
        assertEquals("Temuco", ubicacion["comuna"].asString)
        assertEquals("La Araucanía", ubicacion["region"].asString)
        assertEquals("Centro", ubicacion["sector"].asString)
    }

}
