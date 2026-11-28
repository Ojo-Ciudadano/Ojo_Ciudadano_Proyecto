package cl.uct.ojociudadano.ui.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IncidentMapClusteringTest {

    @Test
    fun `nearby markers are grouped at a far zoom level`() {
        val markers = listOf(
            marker("first", latitude = -38.7359),
            marker("second", latitude = -38.7360)
        )

        val clusters = clusterIncidentMarkers(
            markers = markers,
            zoomLevel = 10.0,
            clusterRadiusPixels = 64.0
        )

        assertEquals(1, clusters.size)
        assertEquals(2, clusters.single().count)
        assertEquals(listOf("first", "second"), clusters.single().incidentIds)
        assertEquals(-38.73595, clusters.single().latitude, 0.000001)
        assertEquals(-72.5904, clusters.single().longitude, 0.000001)
    }

    @Test
    fun `the same markers are separated at a close zoom level`() {
        val markers = listOf(
            marker("first", latitude = -38.7359),
            marker("second", latitude = -38.7360)
        )

        val clusters = clusterIncidentMarkers(
            markers = markers,
            zoomLevel = 22.0,
            clusterRadiusPixels = 16.0
        )

        assertEquals(2, clusters.size)
        assertEquals(listOf("first", "second"), clusters.flatMap { it.incidentIds })
        assertTrue(clusters.all { it.count == 1 })
    }

    @Test
    fun `distant markers never share a cluster`() {
        val clusters = clusterIncidentMarkers(
            markers = listOf(
                marker("temuco", latitude = -38.7359, longitude = -72.5904),
                marker("santiago", latitude = -33.4489, longitude = -70.6693)
            ),
            zoomLevel = 10.0,
            clusterRadiusPixels = 64.0
        )

        assertEquals(2, clusters.size)
        assertEquals(listOf("santiago", "temuco"), clusters.flatMap { it.incidentIds }.sorted())
    }

    @Test
    fun `a non positive radius disables clustering`() {
        val markers = listOf(
            marker("first", latitude = -38.7359),
            marker("second", latitude = -38.7360)
        )

        val clusters = clusterIncidentMarkers(
            markers = markers,
            zoomLevel = 10.0,
            clusterRadiusPixels = 0.0
        )

        assertEquals(2, clusters.size)
        assertEquals(listOf("first", "second"), clusters.flatMap { it.incidentIds })
    }

    @Test
    fun `empty input produces no clusters`() {
        assertTrue(
            clusterIncidentMarkers(
                markers = emptyList(),
                zoomLevel = 10.0,
                clusterRadiusPixels = 64.0
            ).isEmpty()
        )
    }

    @Test
    fun `cluster zoom target reveals the individual markers`() {
        val markers = listOf(
            marker("first", latitude = -38.7359),
            marker("second", latitude = -38.7360)
        )
        val cluster = clusterIncidentMarkers(
            markers = markers,
            zoomLevel = 10.0,
            clusterRadiusPixels = 64.0
        ).single()

        val targetZoom = clusterZoomTarget(
            cluster = cluster,
            currentZoom = 10.0,
            clusterRadiusPixels = 64.0,
            maxZoom = 22.0
        )

        assertTrue(targetZoom > 10.0)
        assertTrue(targetZoom <= 22.0)
        assertEquals(
            2,
            clusterIncidentMarkers(
                markers = markers,
                zoomLevel = targetZoom,
                clusterRadiusPixels = 64.0
            ).size
        )
    }

    @Test
    fun `cluster zoom target never exceeds the maximum zoom`() {
        val cluster = clusterIncidentMarkers(
            markers = listOf(
                marker("first", latitude = -38.7359),
                marker("second", latitude = -38.7360)
            ),
            zoomLevel = 20.0,
            clusterRadiusPixels = 200.0
        ).single()

        assertEquals(
            21.0,
            clusterZoomTarget(
                cluster = cluster,
                currentZoom = 20.0,
                clusterRadiusPixels = 200.0,
                maxZoom = 21.0
            ),
            0.0
        )
    }

    private fun marker(
        id: String,
        latitude: Double,
        longitude: Double = -72.5904
    ) = IncidentMapMarker(
        incidentId = id,
        latitude = latitude,
        longitude = longitude
    )
}
