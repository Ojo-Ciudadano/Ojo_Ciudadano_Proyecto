package cl.uct.ojociudadano.ui.map

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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
}
