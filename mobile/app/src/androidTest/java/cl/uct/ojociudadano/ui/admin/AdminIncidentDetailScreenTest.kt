package cl.uct.ojociudadano.ui.admin

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import cl.uct.ojociudadano.data.model.CandidateUser
import cl.uct.ojociudadano.data.model.Category
import cl.uct.ojociudadano.data.model.Incident
import cl.uct.ojociudadano.data.model.IncidentAssignment
import cl.uct.ojociudadano.data.model.IncidentLocation
import cl.uct.ojociudadano.data.model.IncidentStateHistory
import cl.uct.ojociudadano.data.model.IncidentStatus
import cl.uct.ojociudadano.ui.reports.IncidentsUiState
import cl.uct.ojociudadano.ui.theme.OjoCiudadanoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AdminIncidentDetailScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun loadingRecoverableErrorRetryAndContentKeepBackAvailable() {
        val state = mutableStateOf(AdminIncidentDetailState(incident.id))
        var retries = 0
        var backs = 0
        compose.setContent {
            OjoCiudadanoTheme {
                AdminIncidentDetailContent(state.value, onBack = { backs++ }, onRetry = {
                    retries++
                    state.value = AdminIncidentDetailState(incident.id, IncidentsUiState.Success, incident)
                })
            }
        }
        compose.onNodeWithText("Cargando incidentes…").assertIsDisplayed()
        compose.onNodeWithText(incident.title).assertDoesNotExist()
        compose.runOnIdle {
            state.value = AdminIncidentDetailState(incident.id, IncidentsUiState.Error("Revisa tu conexión."), canRetry = true)
        }
        compose.onNodeWithText("Revisa tu conexión.").assertIsDisplayed()
        compose.onNodeWithText("Reintentar").performClick()
        compose.onNodeWithText(incident.title).assertIsDisplayed()
        compose.onNodeWithText("Estado: En revisión").performScrollTo().assertIsDisplayed().assertHasNoClickAction()
        compose.onNodeWithText("Reintentar").assertDoesNotExist()
        compose.onNodeWithContentDescription("Volver").performClick()
        assertEquals(1, backs)
        assertEquals(1, retries)
    }

    @Test fun terminalErrorsHaveNoRetryOrStaleIncidentAndAllowReturning() {
        val state = mutableStateOf(AdminIncidentDetailState(incident.id, IncidentsUiState.Success, incident))
        var backs = 0
        compose.setContent {
            OjoCiudadanoTheme {
                AdminIncidentDetailContent(state.value, { backs++ }, { error("Must not retry") })
            }
        }
        for (message in listOf("Tu sesión expiró.", "No tienes autorización.", "El incidente ya no está disponible.",
            "El identificador del incidente no es válido.")) {
            compose.runOnIdle { state.value = AdminIncidentDetailState(incident.id, IncidentsUiState.Error(message)) }
            compose.onNodeWithText(message).assertIsDisplayed()
            compose.onNodeWithText("Reintentar").assertDoesNotExist()
            compose.onNodeWithText(incident.title).assertDoesNotExist()
            compose.onNodeWithContentDescription("Volver").performClick()
        }
        assertEquals(4, backs)
    }

    @Test fun compactDarkScreenScrollsThroughFullDescriptionRealFieldsAndDates() {
        val description = "Descripción extensa del incidente. ".repeat(40) + "Fin de la descripción."
        val row = incident.copy(description = description)
        compose.setContent {
            OjoCiudadanoTheme(darkTheme = true) {
                Box(Modifier.width(320.dp)) {
                    AdminIncidentDetailContent(AdminIncidentDetailState(row.id, IncidentsUiState.Success, row), {}, {})
                }
            }
        }
        compose.onNodeWithText(row.title).assertIsDisplayed()
        for (text in listOf(row.id, "Alumbrado", description, "Plaza", "Centro", "Temuco", "Araucanía",
            "-38.7, -72.5", "Creación", "Última actualización")) {
            compose.onNodeWithText(text).performScrollTo().assertIsDisplayed()
        }
        compose.onNodeWithContentDescription("Volver").assertIsDisplayed()
        compose.onNodeWithText(row.reporterUserId).assertDoesNotExist()
        compose.onNodeWithText("Cambiar estado").assertDoesNotExist()
    }

    @Test fun blankFieldsAndAbsentOptionalPriorityDoNotRenderNullOrTechnicalIds() {
        val row = incident.copy(title = "", description = "", updatedAt = "", createdAt = "invalid",
            location = incident.location.copy(referenceAddress = "", sector = "", commune = "", region = ""))
        compose.setContent {
            OjoCiudadanoTheme {
                AdminIncidentDetailContent(AdminIncidentDetailState(row.id, IncidentsUiState.Success, row), {}, {})
            }
        }
        compose.onNodeWithText("Incidente").assertIsDisplayed()
        compose.onNodeWithText("Descripción no disponible").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("No disponible").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Fecha no disponible").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Última actualización").assertDoesNotExist()
        compose.onNodeWithText("Sector").assertDoesNotExist()
        for (text in listOf("null", "undefined", row.location.id, row.category.id, row.status.id)) {
            compose.onNodeWithText(text).assertDoesNotExist()
        }
    }

    @Test fun historySectionCoversEntriesEmptyErrorAndRetryWithoutHidingTheIncident() {
        val entries = listOf(
            history("h1", "2026-11-02T12:00:00Z", "Se inicia la evaluación"),
            history("h2", "2026-11-03T12:00:00Z", null)
        )
        val state = mutableStateOf(
            AdminIncidentDetailState(
                incident.id, IncidentsUiState.Success, incident,
                historyStatus = IncidentsUiState.Success, history = entries
            )
        )
        var historyRetries = 0
        compose.setContent {
            OjoCiudadanoTheme {
                AdminIncidentDetailContent(
                    state.value,
                    onBack = {},
                    onRetry = { error("The detail must not reload when only the history fails") },
                    onRetryHistory = { historyRetries++ }
                )
            }
        }
        compose.onNodeWithText("Historial de cambios").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Se inicia la evaluación").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Sin observación").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText("Estado: En revisión").assertCountEquals(3)
        compose.onAllNodesWithText("Usuario: ${entries.first().userId}").assertCountEquals(2)

        compose.runOnIdle {
            state.value = AdminIncidentDetailState(
                incident.id, IncidentsUiState.Success, incident, historyStatus = IncidentsUiState.Empty
            )
        }
        compose.onNodeWithText("Este incidente aún no registra cambios de estado.")
            .performScrollTo().assertIsDisplayed()

        compose.runOnIdle {
            state.value = AdminIncidentDetailState(
                incident.id, IncidentsUiState.Success, incident,
                historyStatus = IncidentsUiState.Error("No fue posible cargar el historial."),
                historyCanRetry = true
            )
        }
        compose.onNodeWithText(incident.title).assertIsDisplayed()
        compose.onNodeWithText("No fue posible cargar el historial.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Reintentar").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, historyRetries) }

        compose.runOnIdle {
            state.value = AdminIncidentDetailState(
                incident.id, IncidentsUiState.Success, incident,
                historyStatus = IncidentsUiState.Error("Tu sesión expiró."),
                historyCanRetry = false
            )
        }
        compose.onNodeWithText("Tu sesión expiró.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Reintentar").assertDoesNotExist()
    }

    @Test fun onlyOfficialTransitionsAreShownAndClosedOrUnknownHaveNoActions() {
        val state = mutableStateOf(AdminIncidentDetailState(incident.id, IncidentsUiState.Success, incident))
        compose.setContent {
            OjoCiudadanoTheme {
                AdminIncidentDetailContent(state.value, {}, {}, onSelectStatus = {})
            }
        }
        val matrix = mapOf("Nuevo" to listOf("En revisión"), "En revisión" to listOf("En atención"),
            "En atención" to listOf("Resuelto"), "Resuelto" to listOf("En atención", "Cerrado"),
            "Cerrado" to emptyList(), "unknown" to emptyList())
        for ((source, allowed) in matrix) {
            compose.runOnIdle {
                state.value = state.value.copy(incident = incident.copy(status = incident.status.copy(name = source)))
            }
            for (destination in listOf("Nuevo", "En revisión", "En atención", "Resuelto", "Cerrado")) {
                val node = compose.onNodeWithText("Cambiar a $destination")
                if (destination in allowed) node.performScrollTo().assertIsDisplayed() else node.assertDoesNotExist()
            }
            if (allowed.isEmpty()) compose.onNodeWithText("Cambiar estado").assertDoesNotExist()
        }
    }

    @Test fun confirmationRequiresObservationCanCancelAndDisablesActionsWhileSubmitting() {
        val state = mutableStateOf(AdminIncidentDetailState(incident.id, IncidentsUiState.Success, incident))
        var confirmations = 0
        compose.setContent {
            OjoCiudadanoTheme {
                AdminIncidentDetailContent(state.value, {}, {},
                    onSelectStatus = { state.value = state.value.copy(selectedStatus = it) },
                    onObservationChanged = { state.value = state.value.copy(observation = it) },
                    onCancelChange = { state.value = state.value.copy(selectedStatus = null, observation = "") },
                    onConfirmChange = {
                        confirmations++
                        state.value = state.value.copy(submitting = true)
                    })
            }
        }
        compose.onNodeWithText("Cambiar a En atención").performScrollTo().performClick()
        compose.onNodeWithText("Estado actual: En revisión").assertIsDisplayed()
        compose.onNodeWithText("Nuevo estado: En atención").assertIsDisplayed()
        compose.onNodeWithText("Confirmar").assertIsNotEnabled()
        compose.onNodeWithText("Cancelar").performClick()
        compose.onNodeWithText("Confirmar cambio de estado").assertDoesNotExist()
        compose.onNodeWithText("Cambiar a En atención").performClick()
        compose.onNodeWithText("Observación obligatoria").performTextInput("  ")
        compose.onNodeWithText("Confirmar").assertIsNotEnabled()
        compose.onNodeWithText("Observación obligatoria").performTextInput("Inspección realizada")
        compose.onNodeWithText("Confirmar").assertIsEnabled().performClick()
        compose.onNodeWithText("Enviando cambio…").assertIsDisplayed()
        compose.onNodeWithText("Confirmar").assertIsNotEnabled()
        compose.onNodeWithText("Cancelar").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(1, confirmations) }
        compose.runOnIdle {
            state.value = state.value.copy(submitting = false, selectedStatus = null,
                incident = incident.copy(status = incident.status.copy(name = "En atención")))
        }
        compose.onNodeWithText("Cambiar a En atención").assertDoesNotExist()
        compose.onNodeWithText("Cambiar a Resuelto").performScrollTo().assertIsDisplayed()
    }

    @Test fun confirmedChangeWithSyncFailureOffersReloadWithoutAnotherStatusAction() {
        val state = mutableStateOf(AdminIncidentDetailState(incident.id, IncidentsUiState.Success, incident,
            successMessage = "Estado actualizado correctamente.", syncError = "El cambio fue confirmado, pero no se pudo recargar el detalle."))
        var reloads = 0
        compose.setContent {
            OjoCiudadanoTheme {
                AdminIncidentDetailContent(state.value, {}, {}, onSelectStatus = {},
                    onRetrySynchronization = { reloads++ })
            }
        }
        compose.onNodeWithText("Estado actualizado correctamente.").assertIsDisplayed()
        compose.onNodeWithText("Recargar detalle").performClick()
        compose.onNodeWithText("Cambiar estado").assertDoesNotExist()
        assertEquals(1, reloads)
    }

    @Test fun assignmentSectionShowsConfirmedResponsibleOrAnAbsentAssignment() {
        val state = mutableStateOf(
            AdminIncidentDetailState(incident.id, IncidentsUiState.Success, incident)
        )
        var opens = 0
        compose.setContent {
            OjoCiudadanoTheme {
                AdminIncidentDetailContent(
                    state = state.value,
                    onBack = {},
                    onRetry = {},
                    assignmentActions = AdminIncidentAssignmentActions(onOpen = { opens++ })
                )
            }
        }
        compose.onNodeWithText("Responsable").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Sin responsable asignado.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("admin_assignment_open").performScrollTo().performClick()
        assertEquals(1, opens)

        compose.runOnIdle {
            state.value = AdminIncidentDetailState(
                assignedIncident.id,
                IncidentsUiState.Success,
                assignedIncident,
                candidates = AssignmentCandidates.Ready(listOf(admin), 1)
            )
        }
        compose.onNodeWithText("Admin Uno").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Reasignar responsable").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Sin responsable asignado.").assertDoesNotExist()
    }

    @Test fun assignmentPickerOnlyEnablesConfirmAfterSelectingAnAdministrator() {
        val state = mutableStateOf(
            AdminIncidentDetailState(
                assignedIncident.id,
                IncidentsUiState.Success,
                assignedIncident,
                pickerVisible = true,
                candidates = AssignmentCandidates.Ready(listOf(admin, otherAdmin), 2)
            )
        )
        var confirms = 0
        var cancels = 0
        compose.setContent {
            OjoCiudadanoTheme {
                AdminIncidentDetailContent(
                    state = state.value,
                    onBack = {},
                    onRetry = {},
                    assignmentActions = AdminIncidentAssignmentActions(
                        onConfirm = { confirms++ },
                        onDismiss = { cancels++ }
                    )
                )
            }
        }
        compose.onNodeWithText("Reasignar responsable").assertIsDisplayed()
        compose.onNodeWithText("Admin Uno").assertIsDisplayed()
        compose.onNodeWithText("admin1@uct.cl").assertIsDisplayed()
        compose.onNodeWithText("Admin Dos").assertIsDisplayed()
        compose.onNodeWithTag("admin_assignment_confirm").assertIsNotEnabled()

        compose.runOnIdle {
            state.value = state.value.copy(selectedCandidateId = admin.id)
        }
        compose.onNodeWithTag("admin_assignment_confirm").assertIsEnabled().performClick()
        assertEquals(1, confirms)

        compose.onNodeWithTag("admin_assignment_cancel").performClick()
        assertEquals(1, cancels)
    }

    @Test fun failedAndEmptyCandidateCatalogsStayControlledInsideThePicker() {
        val state = mutableStateOf(
            AdminIncidentDetailState(
                incident.id,
                IncidentsUiState.Success,
                incident,
                pickerVisible = true,
                candidates = AssignmentCandidates.Failed("No se pudo consultar a los administradores.", true)
            )
        )
        var retries = 0
        compose.setContent {
            OjoCiudadanoTheme {
                AdminIncidentDetailContent(
                    state = state.value,
                    onBack = {},
                    onRetry = {},
                    assignmentActions = AdminIncidentAssignmentActions(onRetryCandidates = { retries++ })
                )
            }
        }
        compose.onNodeWithText("No se pudo consultar a los administradores.").assertIsDisplayed()
        compose.onNodeWithText("Reintentar").performClick()
        assertEquals(1, retries)

        compose.runOnIdle {
            state.value = state.value.copy(candidates = AssignmentCandidates.Ready(emptyList(), 0))
        }
        compose.onNodeWithText("No hay administradores disponibles.").assertIsDisplayed()
        compose.onNodeWithTag("admin_assignment_confirm").assertIsNotEnabled()
        compose.onNodeWithText(admin.id).assertDoesNotExist()
        compose.onNodeWithText(incident.title).assertIsDisplayed()
    }

    @Test fun prioritySectionShowsCurrentLevelOrExplicitAbsence() {
        val state = mutableStateOf(
            AdminIncidentDetailState(incident.id, IncidentsUiState.Success, incident.copy(priorityLevel = 3))
        )
        compose.setContent {
            OjoCiudadanoTheme { AdminIncidentDetailContent(state.value, {}, {}) }
        }
        compose.onNodeWithText("Prioridad actual").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Nivel 3 de 5").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Modificar prioridad").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Prioridad actualizada correctamente.").assertDoesNotExist()
        compose.runOnIdle { state.value = state.value.copy(incident = incident) }
        compose.onNodeWithText("Sin prioridad registrada").performScrollTo().assertIsDisplayed()
    }

    @Test fun priorityDialogListsAllowedLevelsAndRequiresSelection() {
        val state = mutableStateOf(
            AdminIncidentDetailState(
                incident.id, IncidentsUiState.Success, incident.copy(priorityLevel = 3), priorityDialogVisible = true
            )
        )
        var selected = -1
        var confirmed = 0
        val actions = AdminIncidentPriorityActions(
            onSelect = {
                selected = it
                state.value = state.value.copy(selectedPriority = it)
            },
            onConfirm = { confirmed++ }
        )
        compose.setContent {
            OjoCiudadanoTheme { AdminIncidentDetailContent(state.value, {}, {}, priorityActions = actions) }
        }
        compose.onNodeWithText("Modificar prioridad").assertIsDisplayed()
        for (level in 1..5) compose.onNodeWithTag("admin_priority_option_$level").assertExists()
        compose.onNodeWithTag("admin_priority_confirm").assertIsNotEnabled()
        compose.onNodeWithTag("admin_priority_option_4").performClick()
        compose.runOnIdle { assertEquals(4, selected) }
        compose.onNodeWithTag("admin_priority_confirm").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, confirmed) }
        compose.onNodeWithTag("admin_priority_option_3").performClick()
        compose.runOnIdle { assertEquals(3, selected) }
        compose.onNodeWithTag("admin_priority_confirm").assertIsNotEnabled()
    }

    @Test fun priorityDialogShowsSavingAndRetryableError() {
        val state = mutableStateOf(
            AdminIncidentDetailState(
                incident.id, IncidentsUiState.Success, incident, priorityDialogVisible = true,
                selectedPriority = 2, savingPriority = true
            )
        )
        compose.setContent {
            OjoCiudadanoTheme { AdminIncidentDetailContent(state.value, {}, {}) }
        }
        compose.onNodeWithText("Guardando prioridad…").assertIsDisplayed()
        compose.onNodeWithTag("admin_priority_cancel").assertIsNotEnabled()
        compose.onNodeWithTag("admin_priority_confirm").assertIsNotEnabled()
        compose.runOnIdle {
            state.value = state.value.copy(
                savingPriority = false,
                priorityError = "El servicio de incidentes no está disponible (HTTP 503). Intenta más tarde.",
                priorityCanRetry = true
            )
        }
        compose.onNodeWithText("El servicio de incidentes no está disponible (HTTP 503). Intenta más tarde.")
            .assertIsDisplayed()
        compose.onNodeWithText("Reintentar").assertIsDisplayed()
        compose.onNodeWithTag("admin_priority_confirm").assertIsEnabled()
        compose.onNodeWithTag("admin_priority_cancel").assertIsEnabled()
    }

    @Test fun priorityUpdatedMessageAppearsAfterConfirmation() {
        compose.setContent {
            OjoCiudadanoTheme {
                AdminIncidentDetailContent(
                    AdminIncidentDetailState(
                        incident.id, IncidentsUiState.Success, incident.copy(priorityLevel = 4), priorityUpdated = true
                    ),
                    {}, {}
                )
            }
        }
        compose.onNodeWithText("Nivel 4 de 5").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Prioridad actualizada correctamente.").performScrollTo().assertIsDisplayed()
    }

    private val incident = Incident(
        "d0000000-0000-4000-8000-000000000004", "Luminaria apagada", "Sin iluminación en la plaza",
        Category("category", "Alumbrado", "", 1, false, null),
        IncidentStatus("status", "En revisión", 2),
        IncidentLocation("location", -38.7, -72.5, "Plaza", "Temuco", "Araucanía", "Centro"),
        "another-citizen", null, "2026-11-02T12:00:00Z", "2026-11-03T13:00:00Z"
    )

    private val admin = CandidateUser(
        "c0000000-0000-4000-8000-000000000001",
        "Admin Uno",
        "admin1@uct.cl",
        listOf("ADMIN")
    )

    private val otherAdmin = CandidateUser(
        "c0000000-0000-4000-8000-000000000002",
        "Admin Dos",
        "admin2@uct.cl",
        listOf("ADMIN")
    )

    private val assignedIncident = incident.copy(
        activeAssignment = IncidentAssignment(
            "a0000000-0000-4000-8000-000000000001",
            incident.id,
            admin.id,
            "a0000000-0000-4000-8000-0000000000aa",
            "2026-11-03T10:00:00Z",
            true
        )
    )

    private fun history(id: String, date: String, observation: String?) = IncidentStateHistory(
        id = id,
        incidenteId = incident.id,
        status = IncidentStatus("status-$id", "En revisión", 2),
        userId = "b0000000-0000-4000-8000-000000000012",
        observation = observation,
        date = date
    )
}
