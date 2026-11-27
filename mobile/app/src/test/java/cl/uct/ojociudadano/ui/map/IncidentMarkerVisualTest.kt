package cl.uct.ojociudadano.ui.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class IncidentMarkerVisualTest {

    @Test
    fun `known status names map to their marker status ignoring case and accents`() {
        val expected = mapOf(
            "Nuevo" to IncidentMarkerStatus.NEW,
            "  en revisión " to IncidentMarkerStatus.IN_REVIEW,
            "EN REVISION" to IncidentMarkerStatus.IN_REVIEW,
            "En atención" to IncidentMarkerStatus.IN_ATTENTION,
            "en atencion" to IncidentMarkerStatus.IN_ATTENTION,
            "Resuelto" to IncidentMarkerStatus.RESOLVED,
            "cerrado" to IncidentMarkerStatus.CLOSED
        )

        expected.forEach { (name, status) ->
            assertEquals(name, status, IncidentMarkerStatus.fromBackendName(name))
        }
    }

    @Test
    fun `unknown or missing status falls back to a neutral marker`() {
        assertEquals(IncidentMarkerStatus.UNKNOWN, IncidentMarkerStatus.fromBackendName(null))
        assertEquals(IncidentMarkerStatus.UNKNOWN, IncidentMarkerStatus.fromBackendName(" "))
        assertEquals(IncidentMarkerStatus.UNKNOWN, IncidentMarkerStatus.fromBackendName("Archivado"))
    }

    @Test
    fun `priority levels one to five map to their marker priority`() {
        IncidentMarkerPriority.legendEntries.forEach { priority ->
            assertEquals(priority, IncidentMarkerPriority.fromLevel(priority.level))
        }
    }

    @Test
    fun `missing or out of range priority falls back to unassigned`() {
        assertEquals(IncidentMarkerPriority.UNASSIGNED, IncidentMarkerPriority.fromLevel(null))
        assertEquals(IncidentMarkerPriority.UNASSIGNED, IncidentMarkerPriority.fromLevel(0))
        assertEquals(IncidentMarkerPriority.UNASSIGNED, IncidentMarkerPriority.fromLevel(6))
    }

    @Test
    fun `summary derives both visual dimensions from the loaded incident`() {
        val visual = IncidentMapSummary(
            statusName = "En atención",
            priorityLevel = 4
        ).toMarkerVisual()

        assertEquals(IncidentMarkerStatus.IN_ATTENTION, visual.status)
        assertEquals(IncidentMarkerPriority.HIGH, visual.priority)
        assertFalse(visual.selected)
    }

    @Test
    fun `partial summary keeps the marker drawable with neutral values`() {
        val visual = IncidentMapSummary().toMarkerVisual()

        assertEquals(IncidentMarkerStatus.UNKNOWN, visual.status)
        assertEquals(IncidentMarkerPriority.UNASSIGNED, visual.priority)
    }

    @Test
    fun `selection only changes the highlight flag`() {
        val base = IncidentMapSummary(
            statusName = "Resuelto",
            priorityLevel = 2
        ).toMarkerVisual()

        val selected = base.withSelection(true)

        assertEquals(base.status, selected.status)
        assertEquals(base.priority, selected.priority)
        assertTrue(selected.selected)
        assertSame(selected, selected.withSelection(true))
    }

    @Test
    fun `legend lists every real status and priority in order`() {
        assertEquals(
            listOf(
                IncidentMarkerStatus.NEW,
                IncidentMarkerStatus.IN_REVIEW,
                IncidentMarkerStatus.IN_ATTENTION,
                IncidentMarkerStatus.RESOLVED,
                IncidentMarkerStatus.CLOSED
            ),
            IncidentMarkerStatus.legendEntries
        )
        assertEquals(
            listOf(1, 2, 3, 4, 5),
            IncidentMarkerPriority.legendEntries.map { it.level }
        )
    }
}
