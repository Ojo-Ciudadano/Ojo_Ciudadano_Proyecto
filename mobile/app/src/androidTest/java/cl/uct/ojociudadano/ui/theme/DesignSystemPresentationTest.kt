package cl.uct.ojociudadano.ui.theme

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import cl.uct.ojociudadano.data.model.*
import cl.uct.ojociudadano.ui.admin.*
import cl.uct.ojociudadano.ui.components.CitizenHeader
import cl.uct.ojociudadano.ui.map.IncidentSummaryCard
import cl.uct.ojociudadano.ui.map.toMapMarkerOrNull
import cl.uct.ojociudadano.ui.reports.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Same fixture in real role views, with no network dependency, at 320dp. */
class DesignSystemPresentationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun sameIncidentHasReadableLabelsInBothListsMapAndDetails() {
        val scene = mutableIntStateOf(0)
        val darkTheme = mutableStateOf(false)
        var backs = 0
        compose.setContent {
            OjoCiudadanoTheme(darkTheme = darkTheme.value) {
                Surface(Modifier.width(320.dp).fillMaxHeight()) {
                    when (scene.intValue) {
                        0 -> Column {
                            CitizenHeader("Mis reportes", onBack = { backs++ })
                            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                                IncidentCard(incident) {}
                            }
                        }
                        1 -> IncidentSummaryCard(incident.toMapMarkerOrNull()!!, {}, {})
                        2 -> AdminIncidentsContent(
                            AdminIncidentsState(status = IncidentsUiState.Success, incidents = listOf(incident)), {}, {}
                        )
                        3 -> AdminIncidentDetailContent(
                            AdminIncidentDetailState(incident.id, IncidentsUiState.Success, incident), {}, {}
                        )
                        else -> Column {
                            CitizenHeader("Detalle del incidente", onBack = {})
                            Column(Modifier.verticalScroll(rememberScrollState()).padding(Spacing.page),
                                verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
                                IncidentInformation(incident)
                            }
                        }
                    }
                }
            }
        }
        compose.onNodeWithContentDescription("Volver")
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(1, backs)
        val scenes = listOf("citizen-list", "map-summary", "admin-list", "admin-detail", "citizen-detail")
        for (dark in listOf(false, true)) {
            val theme = if (dark) "dark" else "light"
            compose.runOnIdle { darkTheme.value = dark }
            for ((index, name) in scenes.withIndex()) {
                compose.runOnIdle { scene.intValue = index }
                compose.onNodeWithText("Estado: En revisión", useUnmergedTree = true).apply {
                    if (index != 1) performScrollTo()
                }.assertIsDisplayed().assertHasNoClickAction()
                capture("issue296-$theme-$name-state")
                compose.onNodeWithText("Prioridad: 5 · Crítica", useUnmergedTree = true).apply {
                    if (index != 1) performScrollTo()
                }.assertIsDisplayed().assertHasNoClickAction()
                if (index == 3) compose.onNodeWithText("Modificar prioridad").performScrollTo()
                capture("issue296-$theme-$name-priority")
            }
        }
    }

    private fun capture(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        java.io.File(compose.activity.cacheDir, "$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private val incident = Incident(
        id = "design-295", title = "Luminaria sin funcionamiento",
        description = "La luminaria de la plaza necesita revisión.",
        category = Category("lighting", "Alumbrado", "", 1, false, null),
        status = IncidentStatus("review", "En revisión", 2),
        location = IncidentLocation("location", -38.7359, -72.5904, "Plaza", "Temuco", "Araucanía", "Centro"),
        reporterUserId = "citizen", priorityLevel = 5,
        createdAt = "2026-11-20T12:00:00Z", updatedAt = "2026-11-20T12:00:00Z"
    )
}
