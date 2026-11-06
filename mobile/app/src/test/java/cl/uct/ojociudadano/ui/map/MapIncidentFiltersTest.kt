package cl.uct.ojociudadano.ui.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapIncidentFiltersTest {

    @Test
    fun `without filters every loaded marker remains visible`() {
        val markers = listOf(marker("first", category = "lighting"), marker("second", category = "waste"))

        assertEquals(markers, markers.filteredBy(MapIncidentFilters()))
    }

    @Test
    fun `category filter keeps only matching markers`() {
        val markers = listOf(
            marker("first", category = "lighting"),
            marker("second", category = "waste"),
            marker("third", category = "lighting")
        )

        val visible = markers.filteredBy(MapIncidentFilters(categoryId = "lighting"))

        assertEquals(listOf("first", "third"), visible.map { it.incidentId })
    }

    @Test
    fun `status filter keeps only matching markers`() {
        val markers = listOf(
            marker("first", status = "new"),
            marker("second", status = "resolved")
        )

        val visible = markers.filteredBy(MapIncidentFilters(statusId = "resolved"))

        assertEquals(listOf("second"), visible.map { it.incidentId })
    }

    @Test
    fun `category and status filters are combined`() {
        val markers = listOf(
            marker("first", category = "lighting", status = "new"),
            marker("second", category = "waste", status = "new"),
            marker("third", category = "waste", status = "resolved")
        )

        val visible = markers.filteredBy(
            MapIncidentFilters(categoryId = "waste", statusId = "resolved")
        )

        assertEquals(listOf("third"), visible.map { it.incidentId })
    }

    @Test
    fun `active count reflects the selected criteria`() {
        assertEquals(0, MapIncidentFilters().activeCount)
        assertEquals(1, MapIncidentFilters(categoryId = "lighting").activeCount)
        assertEquals(
            2,
            MapIncidentFilters(categoryId = "lighting", statusId = "new").activeCount
        )
    }

    @Test
    fun `markers without the filtered attribute do not match`() {
        val marker = marker("partial", category = null, status = null)

        assertFalse(MapIncidentFilters(categoryId = "lighting").matches(marker))
        assertFalse(MapIncidentFilters(statusId = "new").matches(marker))
        assertTrue(MapIncidentFilters().matches(marker))
    }

    @Test
    fun `available options are unique and sorted by label`() {
        val markers = listOf(
            marker("first", category = "waste", categoryName = "Aseo"),
            marker("second", category = "lighting", categoryName = "Alumbrado"),
            marker("third", category = "waste", categoryName = "Aseo")
        )

        assertEquals(
            listOf(
                MapFilterOption("lighting", "Alumbrado"),
                MapFilterOption("waste", "Aseo")
            ),
            markers.availableCategoryOptions()
        )
    }

    @Test
    fun `status options ignore markers without status`() {
        val markers = listOf(
            marker("first", status = "new", statusName = "Nuevo"),
            marker("second", status = null, statusName = null)
        )

        assertEquals(
            listOf(MapFilterOption("new", "Nuevo")),
            markers.availableStatusOptions()
        )
    }

    @Test
    fun `validation drops selections that are no longer available`() {
        val filters = MapIncidentFilters(categoryId = "waste", statusId = "resolved")

        val validated = filters.validatedAgainst(
            categories = listOf(MapFilterOption("lighting", "Alumbrado")),
            statuses = listOf(MapFilterOption("new", "Nuevo"))
        )

        assertEquals(MapIncidentFilters(), validated)
    }

    private fun marker(
        id: String,
        category: String? = "lighting",
        categoryName: String? = "Alumbrado",
        status: String? = "new",
        statusName: String? = "Nuevo"
    ) = IncidentMapMarker(
        incidentId = id,
        latitude = -38.7359,
        longitude = -72.5904,
        summary = IncidentMapSummary(
            categoryId = category,
            categoryName = categoryName,
            statusId = status,
            statusName = statusName
        )
    )
}
