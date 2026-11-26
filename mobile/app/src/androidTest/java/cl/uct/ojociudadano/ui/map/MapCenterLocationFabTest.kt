package cl.uct.ojociudadano.ui.map

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import cl.uct.ojociudadano.ui.theme.OjoCiudadanoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MapCenterLocationFabTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun idleFabRequestsTheCurrentLocationWhenClicked() {
        var clicks = 0

        compose.setContent {
            OjoCiudadanoTheme {
                MapCenterLocationFab(
                    isLoading = false,
                    onClick = { clicks++ }
                )
            }
        }

        compose.onNodeWithTag(MapTestTags.CENTER_LOCATION_FAB)
            .assertIsDisplayed()
            .performClick()

        compose.runOnIdle { assertEquals(1, clicks) }
    }

    @Test
    fun loadingFabDoesNotStartAnotherRequest() {
        var clicks = 0

        compose.setContent {
            OjoCiudadanoTheme {
                MapCenterLocationFab(
                    isLoading = true,
                    onClick = { clicks++ }
                )
            }
        }

        compose.onNodeWithTag(MapTestTags.CENTER_LOCATION_FAB).performClick()

        compose.runOnIdle { assertEquals(0, clicks) }
    }
}
