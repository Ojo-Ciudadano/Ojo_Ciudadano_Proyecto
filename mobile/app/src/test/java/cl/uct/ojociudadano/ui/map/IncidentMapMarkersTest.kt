package cl.uct.ojociudadano.ui.map

import cl.uct.ojociudadano.data.model.Category
import cl.uct.ojociudadano.data.model.Incident
import cl.uct.ojociudadano.data.model.IncidentLocation
import cl.uct.ojociudadano.data.model.IncidentStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IncidentMapMarkersTest {

    @Test
    fun `incident with valid coordinates keeps its id and position`() {
        val marker = incident(id = "incident-1").toMapMarkerOrNull()

        assertEquals("incident-1", marker?.incidentId)
        assertEquals(-38.7359, marker?.latitude ?: 0.0, 0.0)
        assertEquals(-72.5904, marker?.longitude ?: 0.0, 0.0)
    }

    @Test
    fun `incidents with invalid coordinates do not generate markers`() {
        val invalid = listOf(
            incident(id = "latitude-high").copy(
                location = location(latitude = 90.1, longitude = 0.0)
            ),
            incident(id = "latitude-low").copy(
                location = location(latitude = -90.1, longitude = 0.0)
            ),
            incident(id = "longitude-high").copy(
                location = location(latitude = 0.0, longitude = 180.1)
            ),
            incident(id = "longitude-low").copy(
                location = location(latitude = 0.0, longitude = -180.1)
            ),
            incident(id = "not-a-number").copy(
                location = location(latitude = Double.NaN, longitude = 0.0)
            ),
            incident(id = "infinite").copy(
                location = location(latitude = 0.0, longitude = Double.POSITIVE_INFINITY)
            )
        )

        assertTrue(invalid.toMapMarkers().isEmpty())
        invalid.forEach { assertNull(it.toMapMarkerOrNull()) }
    }

    @Test
    fun `blank incident id is discarded`() {
        assertNull(incident(id = "   ").toMapMarkerOrNull())
    }

    @Test
    fun `duplicate incidents produce a single marker and preserve order`() {
        val markers = listOf(
            incident(id = "first"),
            incident(id = "second"),
            incident(id = "first").copy(
                location = location(latitude = 10.0, longitude = 20.0)
            )
        ).toMapMarkers()

        assertEquals(listOf("first", "second"), markers.map { it.incidentId })
        assertEquals(-38.7359, markers.first().latitude, 0.0)
    }

    @Test
    fun `mixed incidents keep only georeferenced rows`() {
        val markers = listOf(
            incident(id = "valid"),
            incident(id = "invalid").copy(
                location = location(latitude = 200.0, longitude = 200.0)
            )
        ).toMapMarkers()

        assertEquals(listOf("valid"), markers.map { it.incidentId })
    }

    @Test
    fun `marker keeps the summary data needed to identify the incident`() {
        val summary = incident(id = "incident-1").toMapMarkerOrNull()?.summary

        assertEquals("Poste caído", summary?.title)
        assertEquals("Poste caído en la vía pública", summary?.description)
        assertEquals("Alumbrado", summary?.categoryName)
        assertEquals("Nuevo", summary?.statusName)
        assertEquals("2026-11-05T00:00:00Z", summary?.createdAt)
        assertEquals("Plaza", summary?.referenceAddress)
    }

    @Test
    fun `partial incidents keep their marker and normalize blank fields`() {
        val partial = incident(id = "partial").copy(
            title = " ",
            description = "",
            category = Category("category", "  ", "", 1, false, null),
            status = IncidentStatus("status", "", 1),
            createdAt = " ",
            location = location().copy(referenceAddress = " ")
        )

        val marker = partial.toMapMarkerOrNull()

        assertEquals("partial", marker?.incidentId)
        assertEquals(
            IncidentMapSummary(
                title = null,
                description = null,
                categoryName = null,
                statusName = null,
                createdAt = null,
                referenceAddress = null
            ),
            marker?.summary
        )
    }

    private fun incident(id: String) = Incident(
        id = id,
        title = "Poste caído",
        description = "Poste caído en la vía pública",
        category = Category("category", "Alumbrado", "", 1, false, null),
        status = IncidentStatus("status", "Nuevo", 1),
        location = location(),
        reporterUserId = "a0000000-0000-4000-8000-000000000001",
        priorityLevel = null,
        createdAt = "2026-11-05T00:00:00Z",
        updatedAt = "2026-11-05T00:00:00Z"
    )

    private fun location(
        latitude: Double = -38.7359,
        longitude: Double = -72.5904
    ) = IncidentLocation(
        id = "location",
        latitude = latitude,
        longitude = longitude,
        referenceAddress = "Plaza",
        commune = "Temuco",
        region = "La Araucanía",
        sector = "Centro"
    )
}
