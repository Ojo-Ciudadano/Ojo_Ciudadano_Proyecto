package cl.uct.ojociudadano.ui.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapIncidentNavigationTest {

    @Test
    fun `selected marker resolves its own detail identifier`() {
        val state = success(selectedIncidentId = "second", ids = listOf("first", "second"))

        assertEquals("second", state.selectedDetailIncidentId("second"))
    }

    @Test
    fun `request from a previously selected marker is rejected`() {
        val state = success(selectedIncidentId = "second", ids = listOf("first", "second"))

        assertNull(state.selectedDetailIncidentId("first"))
    }

    @Test
    fun `blank and missing identifiers cannot open a detail`() {
        val state = success(selectedIncidentId = "second", ids = listOf("first", "second"))

        assertNull(state.selectedDetailIncidentId(null))
        assertNull(state.selectedDetailIncidentId(""))
        assertNull(state.selectedDetailIncidentId("   "))
    }

    @Test
    fun `request is rejected when there is no active selection`() {
        val state = success(selectedIncidentId = null, ids = listOf("first", "second"))

        assertNull(state.selectedDetailIncidentId("second"))
    }

    @Test
    fun `surrounding whitespace is normalized before comparing the selection`() {
        val state = success(selectedIncidentId = "second", ids = listOf("first", "second"))

        assertEquals("second", state.selectedDetailIncidentId("  second  "))
    }

    @Test
    fun `marker with a blank identifier never navigates`() {
        val state = MapUiState.Success(
            markers = listOf(marker("   "), marker("second")),
            selectedIncidentId = "   "
        )

        assertNull(state.selectedDetailIncidentId("   "))
    }

    private fun success(selectedIncidentId: String?, ids: List<String>) = MapUiState.Success(
        markers = ids.map(::marker),
        selectedIncidentId = selectedIncidentId
    )

    private fun marker(id: String) = IncidentMapMarker(
        incidentId = id,
        latitude = -38.7359,
        longitude = -72.5904
    )
}
