package cl.uct.ojociudadano.ui.map

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MapScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun mapScreen_rendersCorrectlyWithoutCrashing() {
        // Lanzamos la pantalla del mapa en un entorno de Compose
        composeTestRule.setContent {
            MapScreen(
                modifier = Modifier.testTag("map_screen_tag")
            )
        }

        // Verificamos que el nodo principal de la pantalla del mapa esté presente y visible
        composeTestRule
            .onNodeWithTag("map_screen_tag")
            .assertIsDisplayed()
    }
}