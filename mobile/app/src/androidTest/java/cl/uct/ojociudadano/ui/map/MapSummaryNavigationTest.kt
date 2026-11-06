package cl.uct.ojociudadano.ui.map

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import cl.uct.ojociudadano.ui.theme.OjoCiudadanoTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MapSummaryNavigationTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun summaryOpensTheCurrentlySelectedMarkerEvenAfterSelectionChanges() {
        var opened: String? = null
        var selectedMarker by mutableStateOf(marker("first"))

        compose.setContent {
            OjoCiudadanoTheme {
                IncidentSummaryCard(
                    marker = selectedMarker,
                    onClose = {},
                    onOpenDetail = { opened = it }
                )
            }
        }

        compose.onNodeWithTag(MapTestTags.SUMMARY_DETAIL)
            .assertIsDisplayed()
            .performClick()
        compose.runOnIdle { assertEquals("first", opened) }

        compose.runOnIdle { selectedMarker = marker("second") }
        compose.onNodeWithTag(MapTestTags.SUMMARY_DETAIL).performClick()
        compose.runOnIdle { assertEquals("second", opened) }
    }

    @Test
    fun closingTheSummaryDoesNotOpenTheDetail() {
        var closed = false
        var opened: String? = null

        compose.setContent {
            OjoCiudadanoTheme {
                IncidentSummaryCard(
                    marker = marker("first"),
                    onClose = { closed = true },
                    onOpenDetail = { opened = it }
                )
            }
        }

        compose.onNodeWithTag(MapTestTags.SUMMARY_CLOSE).performClick()

        compose.runOnIdle {
            assertTrue(closed)
            assertNull(opened)
        }
    }

    private fun marker(id: String) = IncidentMapMarker(
        incidentId = id,
        latitude = -38.7359,
        longitude = -72.5904
    )
}
