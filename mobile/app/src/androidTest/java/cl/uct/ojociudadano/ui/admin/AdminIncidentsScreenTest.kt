package cl.uct.ojociudadano.ui.admin

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import cl.uct.ojociudadano.data.model.Category
import cl.uct.ojociudadano.data.model.Incident
import cl.uct.ojociudadano.data.model.IncidentLocation
import cl.uct.ojociudadano.data.model.IncidentStatus
import cl.uct.ojociudadano.ui.reports.IncidentsUiState
import cl.uct.ojociudadano.ui.theme.OjoCiudadanoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AdminIncidentsScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun loadingEmptyErrorAndRetryKeepBackAvailable() {
        val state = mutableStateOf(AdminIncidentsState())
        var backs = 0
        var retries = 0
        compose.setContent {
            OjoCiudadanoTheme {
                AdminIncidentsContent(state.value, onBack = { backs++ }, onRetry = {
                    retries++
                    state.value = AdminIncidentsState(IncidentsUiState.Success, listOf(incident))
                })
            }
        }
        compose.onNodeWithText("Cargando incidentes…").assertIsDisplayed()
        compose.runOnIdle { state.value = AdminIncidentsState(IncidentsUiState.Empty) }
        compose.onNodeWithText("Aún no hay incidentes para mostrar.").assertIsDisplayed()
        compose.onNodeWithText("Cargando incidentes…").assertDoesNotExist()
        compose.runOnIdle {
            state.value = AdminIncidentsState(IncidentsUiState.Error("Revisa tu conexión."))
        }
        compose.onNodeWithText("Aún no hay incidentes para mostrar.").assertDoesNotExist()
        compose.onNodeWithText("Revisa tu conexión.").assertIsDisplayed()
        compose.onNodeWithText("Reintentar").performScrollTo().performClick()
        compose.onNodeWithText(incident.title).assertIsDisplayed()
        compose.onNodeWithContentDescription("Volver").performClick()
        assertEquals(1, backs)
        assertEquals(1, retries)
    }

    @Test fun compactListShowsSummaryAndRealIdWithoutDeadDetailAction() {
        compose.setContent {
            OjoCiudadanoTheme {
                Box(Modifier.width(320.dp)) {
                    AdminIncidentsContent(
                        AdminIncidentsState(IncidentsUiState.Success, listOf(incident)), {}, {}
                    )
                }
            }
        }
        compose.onNodeWithText(incident.title).assertIsDisplayed().assertHasNoClickAction()
        compose.onNodeWithText("Alumbrado").assertIsDisplayed()
        compose.onNodeWithText("Estado: Nuevo").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Plaza · Centro · Temuco").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("ID: ${incident.id}").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Ver detalle").assertDoesNotExist()
    }

    @Test fun selectionPassesRealIdToDetailCallback() {
        var selected: String? = null
        compose.setContent {
            OjoCiudadanoTheme {
                AdminIncidentsContent(
                    AdminIncidentsState(IncidentsUiState.Success, listOf(incident)), {}, {},
                    onIncidentSelected = { selected = it }
                )
            }
        }
        compose.onNodeWithText(incident.title).performClick()
        assertEquals(incident.id, selected)
    }

    @Test fun missingOptionalPriorityBlankLocationAndInvalidDateDoNotBreakSummary() {
        val row = incident.copy(
            description = "", priorityLevel = null, createdAt = "invalid",
            location = incident.location.copy(referenceAddress = "", commune = "", sector = "")
        )
        compose.setContent {
            OjoCiudadanoTheme {
                AdminIncidentsContent(AdminIncidentsState(IncidentsUiState.Success, listOf(row)), {}, {})
            }
        }
        compose.onNodeWithText("Fecha no disponible").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("ID: ${row.id}").performScrollTo().assertIsDisplayed()
    }

    private val incident = Incident(
        "d0000000-0000-4000-8000-000000000004", "Luminaria apagada", "Sin iluminación en la plaza",
        Category("category", "Alumbrado", "", 1, false, null),
        IncidentStatus("status", "Nuevo", 1),
        IncidentLocation("location", -38.7, -72.5, "Plaza", "Temuco", "Araucanía", "Centro"),
        "another-citizen", null, "2026-11-02T12:00:00Z", "2026-11-02T12:00:00Z"
    )
}
