package cl.uct.ojociudadano.ui.map

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.graphics.toArgb
import cl.uct.ojociudadano.ui.theme.incidentStatusTone
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IncidentMarkerIconCacheTest {

    private val resources
        get() = InstrumentationRegistry.getInstrumentation().targetContext.resources

    @Test
    fun repeatedVisualsReuseTheSameRenderedIconInstance() {
        val cache = IncidentMarkerIconCache(resources)
        val visual = IncidentMarkerVisual(
            status = IncidentMarkerStatus.IN_ATTENTION,
            priority = IncidentMarkerPriority.HIGH
        )

        val first = cache.iconFor(visual)
        val second = cache.iconFor(visual)

        assertSame(first, second)
        assertEquals(1, cache.cachedIconCount)
        assertTrue(first.intrinsicWidth > 0)
        assertTrue(first.intrinsicHeight > 0)
    }

    @Test
    fun statusPriorityAndSelectionProduceIndependentIcons() {
        val cache = IncidentMarkerIconCache(resources)
        val base = IncidentMarkerVisual(
            status = IncidentMarkerStatus.NEW,
            priority = IncidentMarkerPriority.MEDIUM
        )

        val baseIcon = cache.iconFor(base)
        val otherStatus = cache.iconFor(base.copy(status = IncidentMarkerStatus.RESOLVED))
        val otherPriority = cache.iconFor(base.copy(priority = IncidentMarkerPriority.CRITICAL))
        val selected = cache.iconFor(base.copy(selected = true))

        assertNotSame(baseIcon, otherStatus)
        assertNotSame(baseIcon, otherPriority)
        assertNotSame(baseIcon, selected)
        assertEquals(4, cache.cachedIconCount)
    }

    @Test
    fun selectedIconIsLargerThanTheRegularOne() {
        val cache = IncidentMarkerIconCache(resources)
        val regular = cache.iconFor(
            IncidentMarkerVisual(
                status = IncidentMarkerStatus.CLOSED,
                priority = IncidentMarkerPriority.UNASSIGNED
            )
        )
        val selected = cache.iconFor(
            IncidentMarkerVisual(
                status = IncidentMarkerStatus.CLOSED,
                priority = IncidentMarkerPriority.UNASSIGNED,
                selected = true
            )
        )

        assertTrue(selected.intrinsicWidth > regular.intrinsicWidth)
        assertTrue(selected.intrinsicHeight > regular.intrinsicHeight)
    }

    @Test
    fun repeatedClusterCountsReuseTheSameRenderedIconInstance() {
        val cache = IncidentClusterIconCache(resources)

        val first = cache.iconFor(12)
        val second = cache.iconFor(12)
        val other = cache.iconFor(13)

        assertSame(first, second)
        assertNotSame(first, other)
        assertEquals(2, cache.cachedIconCount)
        assertTrue(first.intrinsicWidth > 0)
        assertTrue(first.intrinsicHeight > 0)
    }

    @Test
    fun clusterIconUsesTheInstitutionalBlueFill() {
        val cache = IncidentClusterIconCache(resources)
        val bitmap = cache.iconFor(5).bitmap

        val fillPixel = bitmap.getPixel(
            (bitmap.width * 0.25f).roundToInt(),
            bitmap.height / 2
        )

        assertEquals(
            cl.uct.ojociudadano.ui.theme.CitizenBlue.toArgb(),
            fillPixel
        )
    }

    @Test
    fun markerFillUsesTheSameStatusColorAsTheStateBadges() {
        val cache = IncidentMarkerIconCache(resources)

        IncidentMarkerStatus.legendEntries.forEach { status ->
            val bitmap = cache.iconFor(
                IncidentMarkerVisual(
                    status = status,
                    priority = IncidentMarkerPriority.MEDIUM
                )
            ).bitmap

            val fillPixel = bitmap.getPixel(
                bitmap.width / 2,
                (bitmap.height * 0.18f).roundToInt()
            )

            assertEquals(
                "Color de relleno incorrecto para $status",
                incidentStatusTone(status.backendName).container.toArgb(),
                fillPixel
            )
        }
    }

    @Test
    fun pinSilhouetteIsFilledFromTheCircleToTheTip() {
        val cache = IncidentMarkerIconCache(resources)
        val bitmap = cache.iconFor(
            IncidentMarkerVisual(
                status = IncidentMarkerStatus.RESOLVED,
                priority = IncidentMarkerPriority.HIGH
            )
        ).bitmap

        val bodyPixel = bitmap.getPixel(
            bitmap.width / 2,
            (bitmap.height * 0.63f).roundToInt()
        )

        assertEquals(
            incidentStatusTone(IncidentMarkerStatus.RESOLVED.backendName).container.toArgb(),
            bodyPixel
        )
    }
}
