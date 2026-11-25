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

class MapLegendTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun toggleReportsWhenTheUserWantsToOpenOrCloseTheLegend() {
        var clicks = 0

        compose.setContent {
            OjoCiudadanoTheme {
                MapLegendToggle(expanded = false, onClick = { clicks++ })
            }
        }

        compose.onNodeWithTag(MapTestTags.LEGEND_TOGGLE)
            .assertIsDisplayed()
            .performClick()

        compose.runOnIdle { assertEquals(1, clicks) }
    }

    @Test
    fun expandedLegendListsEveryStatusColorAndPriorityGlyph() {
        compose.setContent {
            OjoCiudadanoTheme {
                MapLegendPanel(onClose = {})
            }
        }

        compose.onNodeWithTag(MapTestTags.LEGEND_PANEL).assertIsDisplayed()
        IncidentMarkerStatus.legendEntries.forEach { status ->
            compose.onNodeWithTag(MapTestTags.LEGEND_STATUS_PREFIX + status.name)
                .assertExists()
        }
        IncidentMarkerPriority.legendEntries.forEach { priority ->
            compose.onNodeWithTag(MapTestTags.LEGEND_PRIORITY_PREFIX + priority.name)
                .assertExists()
        }
    }

    @Test
    fun legendCloseButtonInvokesItsCallback() {
        var closed = false

        compose.setContent {
            OjoCiudadanoTheme {
                MapLegendPanel(onClose = { closed = true })
            }
        }

        compose.onNodeWithTag(MapTestTags.LEGEND_CLOSE).performClick()

        compose.runOnIdle { assertTrue(closed) }
    }
}
