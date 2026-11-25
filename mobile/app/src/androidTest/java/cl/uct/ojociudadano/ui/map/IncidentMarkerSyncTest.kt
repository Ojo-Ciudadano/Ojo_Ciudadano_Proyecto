package cl.uct.ojociudadano.ui.map

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
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

    @Test
    fun syncAssignsCustomIconsAndReusesCachedInstances() {
        val cache = IncidentMarkerIconCache(resources)
        val markers = listOf(
            incidentMarker("new", status = "Nuevo", priority = 2),
            incidentMarker("resolved", status = "Resuelto", priority = 5)
        )

        sync(markers, cache = cache)

        val newIcon = markerById("new").icon
        assertNotNull(newIcon)
        assertNotSame(newIcon, markerById("resolved").icon)
        assertEquals(2, cache.cachedIconCount)

        sync(markers, cache = cache)

        assertSame(newIcon, markerById("new").icon)
        assertEquals(2, cache.cachedIconCount)
    }

    @Test
    fun selectedMarkerUsesHighlightedIconAndIsDrawnLast() {
        val cache = IncidentMarkerIconCache(resources)
        val markers = listOf(
            incidentMarker("first", status = "Nuevo", priority = 1),
            incidentMarker("second", status = "En revisión", priority = 3)
        )

        sync(markers, selectedIncidentId = "first", cache = cache)

        val selectedIcon = markerById("first").icon
        assertNotSame(selectedIcon, markerById("second").icon)
        assertTrue(selectedIcon.intrinsicWidth > markerById("second").icon.intrinsicWidth)
        assertEquals("first", (mapView.overlays.last() as Marker).relatedObject)

        sync(markers, selectedIncidentId = "second", cache = cache)

        assertNotSame(selectedIcon, markerById("first").icon)
        assertTrue(markerById("second").icon.intrinsicWidth > markerById("first").icon.intrinsicWidth)
        assertEquals("second", (mapView.overlays.last() as Marker).relatedObject)
    }

    private val resources
        get() = InstrumentationRegistry.getInstrumentation().targetContext.resources

    private fun incidentMarker(
        id: String,
        status: String? = null,
        priority: Int? = null
    ) = IncidentMapMarker(
        incidentId = id,
        latitude = -38.7359,
        longitude = -72.5904,
        summary = IncidentMapSummary(
            statusName = status,
            priorityLevel = priority
        )
    )

    private fun markerById(id: String): Marker = incidentMarkers().single {
        it.relatedObject == id
    }

    private fun incidentMarkers(): List<Marker> = mapView.overlays.filterIsInstance<Marker>()

    private fun sync(
        markers: List<IncidentMapMarker>,
        selectedIncidentId: String? = null,
        cache: IncidentMarkerIconCache? = null
    ) {
        onMainThread {
            syncIncidentMarkers(
                mapView = mapView,
                desiredMarkers = markers,
                selectedIncidentId = selectedIncidentId,
                iconCache = cache ?: IncidentMarkerIconProvider.cacheFor(mapView.context)
            )
        }
    }

    private fun onMainThread(block: () -> Unit) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
    }
}
