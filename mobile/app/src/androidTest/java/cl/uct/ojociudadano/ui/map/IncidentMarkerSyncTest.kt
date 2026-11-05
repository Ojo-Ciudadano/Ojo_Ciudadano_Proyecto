package cl.uct.ojociudadano.ui.map

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.config.Configuration
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@RunWith(AndroidJUnit4::class)
class IncidentMarkerSyncTest {

    private lateinit var mapView: MapView

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        onMainThread {
            Configuration.getInstance().load(
                context,
                context.getSharedPreferences("osmdroid-test", 0)
            )
            mapView = MapView(context)
        }
    }

    @Test
    fun syncCreatesOneMarkerPerIncidentAndKeepsItsId() {
        sync(
            listOf(
                IncidentMapMarker("first", -38.7359, -72.5904),
                IncidentMapMarker("second", -38.74, -72.60)
            )
        )

        val markers = incidentMarkers()
        assertEquals(2, markers.size)
        assertEquals(setOf("first", "second"), markers.map { it.relatedObject }.toSet())
        assertNotNull(markers.firstOrNull { it.relatedObject == "first" }?.position)
    }

    @Test
    fun resyncIngthesaMedataDoesNotDuplicateMarkers() {
        val markers = listOf(IncidentMapMarker("first", -38.7359, -72.5904))

        repeat(3) { sync(markers) }

        assertEquals(1, incidentMarkers().size)
    }

    @Test
    fun syncRemovesStaleMarkersAndUpdatesExistingPositions() {
        sync(
            listOf(
                IncidentMapMarker("keep", -38.7359, -72.5904),
                IncidentMapMarker("remove", -38.74, -72.60)
            )
        )

        sync(listOf(IncidentMapMarker("keep", 10.0, 20.0)))

        val markers = incidentMarkers()
        assertEquals(1, markers.size)
        assertEquals("keep", markers.single().relatedObject)
        assertEquals(10.0, markers.single().position.latitude, 0.0)
        assertEquals(20.0, markers.single().position.longitude, 0.0)
    }

    @Test
    fun syncRemovesDuplicateMarkersAlreadyPresentInTheMap() {
        val duplicate = IncidentMapMarker("duplicated", -38.7359, -72.5904)

        onMainThread {
            repeat(2) {
                mapView.overlays.add(
                    Marker(mapView).apply {
                        position = org.osmdroid.util.GeoPoint(
                            duplicate.latitude,
                            duplicate.longitude
                        )
                        relatedObject = duplicate.incidentId
                    }
                )
            }
        }

        sync(listOf(duplicate))

        assertEquals(1, incidentMarkers().size)
    }

    private fun incidentMarkers(): List<Marker> = mapView.overlays.filterIsInstance<Marker>()

    private fun sync(markers: List<IncidentMapMarker>) {
        onMainThread { syncIncidentMarkers(mapView, markers) }
    }

    private fun onMainThread(block: () -> Unit) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
    }
}
