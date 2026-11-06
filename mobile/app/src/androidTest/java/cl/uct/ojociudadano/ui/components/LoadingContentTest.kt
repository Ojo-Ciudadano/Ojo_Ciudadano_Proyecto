package cl.uct.ojociudadano.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import cl.uct.ojociudadano.ui.theme.OjoCiudadanoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LoadingContentTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun indicadorCentradoEnElContenidoDeScaffoldConCabecerasYDistintosAnchos() {
        val compact = mutableStateOf(false)
        compose.setContent {
            OjoCiudadanoTheme(darkTheme = compact.value) {
                Box(Modifier.width(if (compact.value) 320.dp else 380.dp).height(520.dp)) {
                    Scaffold(
                        topBar = { Box(Modifier.fillMaxWidth().height(72.dp)) },
                        bottomBar = { Box(Modifier.fillMaxWidth().height(56.dp)) }
                    ) { padding ->
                        Box(Modifier.fillMaxSize().padding(padding).testTag("area")) {
                            LoadingContent(modifier = Modifier.fillMaxSize(),
                                indicatorModifier = Modifier.testTag("spinner"))
                        }
                    }
                }
            }
        }
        for (small in listOf(false, true)) {
            compose.runOnIdle { compact.value = small }
            val area = compose.onNodeWithTag("area").fetchSemanticsNode().boundsInRoot
            val spinner = compose.onNodeWithTag("spinner").fetchSemanticsNode().boundsInRoot
            assertEquals(area.center.x, spinner.center.x, 1f)
            assertEquals(area.center.y, spinner.center.y, 1f)
        }
    }
}
