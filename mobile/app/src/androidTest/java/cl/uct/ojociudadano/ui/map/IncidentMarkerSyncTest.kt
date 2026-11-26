package cl.uct.ojociudadano.ui.map

import android.graphics.drawable.BitmapDrawable
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cl.uct.ojociudadano.domain.model.GeoCoordinate
import cl.uct.ojociudadano.ui.theme.IncidentTones
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.config.Configuration
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@RunWith(AndroidJUnit4::class)
class IncidentMarkerSyncTest {

    private lateinit var mapView: MapView
    private lateinit var lightIcons: IncidentMarkerIcons

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        onMainThread {
            Configuration.getInstance().load(
                context,
                context.getSharedPreferences("osmdroid-test", 0)
            )
            mapView = MapView(context)
            lightIcons = IncidentMarkerIcons(context.resources, IncidentTones.Light)
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
    fun changingThemeRecolorsExistingMarkerInPlace() {
        val data = IncidentMapMarker("first", -38.7359, -72.5904,
            IncidentMapSummary(statusName = "En revisión", priorityLevel = 3))
        sync(listOf(data))
        val marker = incidentMarkers().single()
        val lightBitmap = (marker.icon as BitmapDrawable).bitmap
        onMainThread {
            val darkIcons = IncidentMarkerIcons(mapView.resources, IncidentTones.Dark)
            syncIncidentMarkers(mapView, listOf(data), darkIcons)
        }
        assertSame(marker, incidentMarkers().single())
        val darkBitmap = (marker.icon as BitmapDrawable).bitmap
        org.junit.Assert.assertFalse(lightBitmap.sameAs(darkBitmap))
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

    @Test
    fun userLocationMarkerIsCreatedOnceAndUpdatedInPlace() {
        onMainThread {
            syncUserLocationMarker(mapView, userLocation(-38.7359, -72.5904))
        }

        var userMarkers = markerList().filter { it.title == "Tu ubicación actual" }
        assertEquals(1, userMarkers.size)
        assertEquals(-38.7359, userMarkers.single().position.latitude, 0.0)
        assertEquals(-72.5904, userMarkers.single().position.longitude, 0.0)

        onMainThread {
            syncUserLocationMarker(mapView, userLocation(-38.74, -72.60))
        }

        userMarkers = markerList().filter { it.title == "Tu ubicación actual" }
        assertEquals(1, userMarkers.size)
        assertEquals(-38.74, userMarkers.single().position.latitude, 0.0)
    }

    @Test
    fun incidentSyncDoesNotRemoveTheUserLocationMarker() {
        onMainThread {
            syncUserLocationMarker(mapView, userLocation(-38.7359, -72.5904))
            syncIncidentMarkers(
                mapView,
                listOf(IncidentMapMarker("first", -38.7359, -72.5904)),
                lightIcons
            )
        }

        assertEquals(1, incidentMarkers().size)
        assertEquals(1, markerList().count { it.title == "Tu ubicación actual" })

        onMainThread { syncIncidentMarkers(mapView, emptyList(), lightIcons) }

        assertEquals(0, incidentMarkers().size)
        assertEquals(1, markerList().count { it.title == "Tu ubicación actual" })
    }

    @Test
    fun removingTheUserLocationClearsItsMarker() {
        onMainThread {
            syncUserLocationMarker(mapView, userLocation(-38.7359, -72.5904))
            syncUserLocationMarker(mapView, null)
        }

        assertEquals(0, markerList().count { it.title == "Tu ubicación actual" })
    }

    private fun userLocation(latitude: Double, longitude: Double) = MapUserLocation(
        coordinate = GeoCoordinate(
            latitude = latitude,
            longitude = longitude
        )
    )

    private fun markerList(): List<Marker> = mapView.overlays.filterIsInstance<Marker>()

    private fun incidentMarkers(): List<Marker> =
        markerList().filter { it.relatedObject is String }

    private fun sync(markers: List<IncidentMapMarker>) {
        onMainThread { syncIncidentMarkers(mapView, markers, lightIcons) }
    }

    private fun onMainThread(block: () -> Unit) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
    }
}
