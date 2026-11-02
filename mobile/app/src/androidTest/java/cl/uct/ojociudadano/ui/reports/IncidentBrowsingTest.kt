package cl.uct.ojociudadano.ui.reports

import androidx.activity.ComponentActivity
import androidx.compose.runtime.remember
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import cl.uct.ojociudadano.data.model.*
import cl.uct.ojociudadano.data.remote.error.ApiException
import cl.uct.ojociudadano.data.repository.CategoryRepository
import cl.uct.ojociudadano.data.repository.IncidentRepository
import cl.uct.ojociudadano.domain.model.Evidence
import cl.uct.ojociudadano.ui.theme.OjoCiudadanoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class IncidentBrowsingTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val repository = Repository()

    private fun show(onOpen: (String) -> Unit = {}, onBack: () -> Unit = {}) {
        compose.setContent {
            val model = remember {
                ReportsViewModel(repository, object : CategoryRepository {
                    override suspend fun getCategories() = emptyList<Category>()
                })
            }
            OjoCiudadanoTheme { ReportsScreen(onBack = onBack, onIncidentClick = onOpen, reportsViewModel = model) }
        }
    }

    @Test fun emptyAndErrorKeepTabsRetryAndBackAvailable() {
        repository.failAll = true
        var backs = 0
        show(onBack = { backs++ })
        compose.onNodeWithText("No hay incidentes en esta sección.").assertIsDisplayed()
        compose.onNodeWithText("Todos los incidentes").performClick()
        compose.onNodeWithText("El servicio de incidentes no está disponible (HTTP 503). Intenta más tarde.").assertIsDisplayed()
        compose.onNodeWithText("No hay incidentes en esta sección.").assertDoesNotExist()
        repository.failAll = false
        compose.onNodeWithText("Reintentar").performScrollTo().performClick()
        compose.onNodeWithText("No hay incidentes en esta sección.").assertIsDisplayed()
        compose.onNodeWithText("Mis incidentes").performClick()
        compose.onNodeWithContentDescription("Volver").performClick()
        assertEquals(1, backs)
    }

    @Test fun authorizedOtherCitizenReportOpensSelectedDetailAndDisappearsInMine() {
        repository.other = true
        var opened = ""
        show(onOpen = { opened = it })
        compose.onNodeWithText("Todos los incidentes").performClick()
        compose.onNodeWithText("Luminaria de prueba").performScrollTo().performClick()
        assertEquals("test-incident", opened)
        compose.onNodeWithText("Mis incidentes").performClick()
        compose.onNodeWithText("Luminaria de prueba").assertDoesNotExist()
    }

    private class Repository : IncidentRepository {
        @Volatile var failAll = false
        var other = false
        override suspend fun getCurrentUserIncidents() = emptyList<Incident>()
        override suspend fun getIncidents(): List<Incident> {
            if (failAll) throw ApiException.Http(503, emptyList(), null, null)
            return if (!other) emptyList() else listOf(Incident(
                "test-incident", "Luminaria de prueba", "Evidencia utilizada exclusivamente en pruebas",
                Category("category", "Alumbrado", "", 1, false, null), IncidentStatus("status", "Nuevo", 1),
                IncidentLocation("location", -38.7, -72.5, "Plaza", "Temuco", "Araucanía", "Centro"),
                "another-citizen", null, "2026-10-28T12:00:00Z", "2026-10-28T12:00:00Z"
            ))
        }
        override suspend fun getIncidentById(id: String): Incident = error("Unused")
        override suspend fun createIncident(input: CreateIncidentInput): Incident = error("Unused")
        override suspend fun uploadEvidence(incidenteId: String, file: java.io.File, mimeType: String): Evidence = error("Unused")
        override suspend fun getEvidencesByIncident(incidenteId: String) = emptyList<Evidence>()
        override suspend fun getEvidenceById(id: String): Evidence = error("Unused")
        override suspend fun downloadEvidenceFile(id: String): okhttp3.ResponseBody = error("Unused")
    }
}
