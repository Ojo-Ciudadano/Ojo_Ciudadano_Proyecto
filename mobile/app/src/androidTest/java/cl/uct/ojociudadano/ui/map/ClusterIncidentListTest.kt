package cl.uct.ojociudadano.ui.map

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import cl.uct.ojociudadano.ui.theme.OjoCiudadanoTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ClusterIncidentListTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun clusterListShowsEveryIncidentAndAllowsSelectingOne() {
        var selected: String? = null
        val markers = listOf(
            marker("first", "Incidente uno"),
            marker("second", "Incidente dos")
        )

        compose.setContent {
            OjoCiudadanoTheme {
                ClusterIncidentListCard(
                    markers = markers,
                    onClose = {},
                    onIncidentClick = { selected = it }
                )
            }
        }

        compose.onNodeWithTag(MapTestTags.CLUSTER_LIST).assertIsDisplayed()
        compose.onNodeWithTag(MapTestTags.CLUSTER_LIST_ITEM_PREFIX + "first")
            .assertIsDisplayed()
            .performClick()

        compose.runOnIdle { assertEquals("first", selected) }
    }

    @Test
    fun clusterListCanBeClosedWithoutSelectingAnIncident() {
        var closed = false
        var selected: String? = null

        compose.setContent {
            OjoCiudadanoTheme {
                ClusterIncidentListCard(
                    markers = listOf(marker("first", "Incidente uno")),
                    onClose = { closed = true },
                    onIncidentClick = { selected = it }
                )
            }
        }

        compose.onNodeWithTag(MapTestTags.CLUSTER_LIST_CLOSE).performClick()

        compose.runOnIdle {
            assertTrue(closed)
            assertEquals(null, selected)
        }
    }

    private fun marker(id: String, title: String) = IncidentMapMarker(
        incidentId = id,
        latitude = -38.7359,
        longitude = -72.5904,
        summary = IncidentMapSummary(
            title = title,
            categoryName = "Vial",
            statusName = "Nuevo",
            priorityLevel = 2,
            referenceAddress = "Av. Alemania 123"
        )
    )
}
