package cl.uct.ojociudadano.ui.notifications

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import cl.uct.ojociudadano.data.model.Notification
import cl.uct.ojociudadano.ui.reports.incidentDate
import cl.uct.ojociudadano.ui.theme.OjoCiudadanoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class NotificationsScreenTest {

    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun loadingShowsProgressAndHidesStaleContent() {
        compose.setContent {
            OjoCiudadanoTheme {
                NotificationsContent(
                    state = NotificationsUiState.Loading,
                    notifications = emptyList(),
                    onBack = {},
                    onRetry = {}
                )
            }
        }
        compose.onNodeWithText("Cargando notificaciones…").assertIsDisplayed()
        compose.onNodeWithText("Tus notificaciones").assertIsDisplayed()
        compose.onNodeWithText(notification.message).assertDoesNotExist()
    }

    @Test fun successRendersMessageDateIncidentReferenceAndUnreadMarker() {
        val unread = notification(id = "unread", isRead = false)
        val read = notification(id = "read", message = "Tu incidente fue cerrado.", isRead = true)
        compose.setContent {
            OjoCiudadanoTheme {
                NotificationsContent(
                    state = NotificationsUiState.Success,
                    notifications = listOf(unread, read),
                    onBack = {},
                    onRetry = {}
                )
            }
        }
        compose.onNodeWithText(unread.message).assertIsDisplayed()
        compose.onNodeWithText(read.message).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(incidentDate(unread.createdAt)).assertIsDisplayed()
        compose.onNodeWithText("Incidente: ${unread.incidentId}").assertIsDisplayed()
        assertEquals(1, compose.onAllNodesWithText("Nueva").fetchSemanticsNodes().size)
    }

    @Test fun missingBackendReferenceAndInvalidDateAreStillRepresentedHonestly() {
        val row = notification(id = "sparse", incidentId = "", createdAt = "not-a-date")
        compose.setContent {
            OjoCiudadanoTheme {
                NotificationsContent(
                    state = NotificationsUiState.Success,
                    notifications = listOf(row),
                    onBack = {},
                    onRetry = {}
                )
            }
        }
        compose.onNodeWithText("Fecha no disponible").assertIsDisplayed()
        compose.onNodeWithText("Incidente: ", substring = true).assertDoesNotExist()
        compose.onNodeWithText("null").assertDoesNotExist()
    }

    @Test fun emptyStateIsValidAndOffersNoRetry() {
        var retries = 0
        compose.setContent {
            OjoCiudadanoTheme {
                NotificationsContent(
                    state = NotificationsUiState.Empty,
                    notifications = emptyList(),
                    onBack = {},
                    onRetry = { retries++ }
                )
            }
        }
        compose.onNodeWithText("Aún no tienes notificaciones. Te avisaremos cuando haya novedades de tus reportes.")
            .assertIsDisplayed()
        compose.onNodeWithText("Reintentar").assertDoesNotExist()
        assertEquals(0, retries)
    }

    @Test fun recoverableErrorExplainsWhatHappenedAndAllowsRetry() {
        var retries = 0
        compose.setContent {
            OjoCiudadanoTheme {
                NotificationsContent(
                    state = NotificationsUiState.Error(
                        "El servicio de notificaciones no está disponible (HTTP 503). Intenta más tarde.",
                        canRetry = true
                    ),
                    notifications = emptyList(),
                    onBack = {},
                    onRetry = { retries++ }
                )
            }
        }
        compose.onNodeWithText("El servicio de notificaciones no está disponible (HTTP 503). Intenta más tarde.")
            .assertIsDisplayed()
        compose.onNodeWithText("Reintentar").performClick()
        assertEquals(1, retries)
    }

    @Test fun terminalErrorDoesNotOfferAnUselessRetry() {
        compose.setContent {
            OjoCiudadanoTheme {
                NotificationsContent(
                    state = NotificationsUiState.Error("Tu sesión expiró. Inicia sesión nuevamente.", canRetry = false),
                    notifications = emptyList(),
                    onBack = {},
                    onRetry = { error("Terminal errors must not retry") }
                )
            }
        }
        compose.onNodeWithText("Tu sesión expiró. Inicia sesión nuevamente.").assertIsDisplayed()
        compose.onNodeWithText("Reintentar").assertDoesNotExist()
    }

    @Test fun backActionAndNarrowLayoutKeepContentReachable() {
        var backs = 0
        val long = notification(
            id = "long",
            message = "Tu incidente fue reasignado a otro administrador. ".repeat(8) + "Fin del aviso."
        )
        compose.setContent {
            OjoCiudadanoTheme {
                Box(Modifier.width(320.dp)) {
                    NotificationsContent(
                        state = NotificationsUiState.Success,
                        notifications = listOf(long),
                        onBack = { backs++ },
                        onRetry = {}
                    )
                }
            }
        }
        compose.onNodeWithText("Incidente: ${long.incidentId}").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Volver").performClick()
        assertEquals(1, backs)
    }

    private val notification = Notification(
        id = "notification-1",
        userId = "a0000000-0000-4000-8000-000000000001",
        incidentId = "d0000000-0000-4000-8000-000000000004",
        message = "El estado de tu incidente cambió a En revisión.",
        isRead = false,
        createdAt = "2026-11-03T08:30:00Z"
    )

    private fun notification(
        id: String,
        message: String = "El estado de tu incidente cambió a En revisión.",
        isRead: Boolean = false,
        incidentId: String = "d0000000-0000-4000-8000-000000000004",
        createdAt: String = "2026-11-03T08:30:00Z"
    ) = Notification(
        id = id,
        userId = "a0000000-0000-4000-8000-000000000001",
        incidentId = incidentId,
        message = message,
        isRead = isRead,
        createdAt = createdAt
    )
}
