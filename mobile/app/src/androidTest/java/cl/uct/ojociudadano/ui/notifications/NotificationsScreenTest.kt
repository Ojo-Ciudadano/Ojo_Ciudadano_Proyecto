package cl.uct.ojociudadano.ui.notifications

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.unit.dp
import cl.uct.ojociudadano.data.model.Notification
import cl.uct.ojociudadano.ui.reports.incidentDate
import cl.uct.ojociudadano.ui.theme.OjoCiudadanoTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        val read = notification(
            id = "read", message = "Tu incidente fue cerrado.", isRead = true,
            createdAt = "2026-11-02T08:30:00Z", incidentId = "another-incident"
        )
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

    @Test fun notificationWithIncidentIsClickableAndWithoutItIsNot() {
        val opened = mutableListOf<String>()
        compose.setContent {
            OjoCiudadanoTheme {
                NotificationsContent(
                    state = NotificationsUiState.Success,
                    notifications = listOf(
                        notification(id = "with-incident"),
                        notification(id = "without-incident", incidentId = "  ")
                    ),
                    onBack = {},
                    onRetry = {},
                    onNotificationClick = { opened += it.id }
                )
            }
        }

        compose.onNodeWithTag("notification_with-incident").performClick()
        compose.onNodeWithTag("notification_without-incident").performClick()

        assertEquals(listOf("with-incident"), opened)
    }

    @Test fun invalidIncidentIsNotMarkedNorNavigatedAndShowsAControlledMessage() {
        val opened = mutableListOf<String>()
        val invalid = mutableListOf<String>()
        var dismissed = false
        compose.setContent {
            OjoCiudadanoTheme {
                NotificationsContent(
                    state = NotificationsUiState.Success,
                    notifications = listOf(
                        notification(id = "broken-incident", incidentId = "incidente/otro")
                    ),
                    onBack = {},
                    onRetry = {},
                    onNotificationClick = { opened += it.id },
                    onInvalidIncident = { invalid += it.id },
                    invalidIncidentMessage = "El incidente asociado a esta notificación no es válido.",
                    onDismissInvalidIncident = { dismissed = true }
                )
            }
        }

        compose.onNodeWithText("El incidente asociado a esta notificación no es válido.")
            .assertIsDisplayed()
        compose.onNodeWithTag("notification_broken-incident").performClick()

        assertEquals(listOf("broken-incident"), invalid)
        assertTrue(opened.isEmpty())

        compose.onNodeWithText("Entendido").performClick()
        assertTrue(dismissed)
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

    @Test fun pullRefreshFromEmptyShowsIndicatorAndKeepsRowsOnLaterError() {
        val state = mutableStateOf<NotificationsUiState>(NotificationsUiState.Empty)
        val rows = mutableStateOf(emptyList<Notification>())
        val refreshing = mutableStateOf(false)
        val indicator = compose.onNode(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ProgressBarRangeInfo,
                ProgressBarRangeInfo.Indeterminate
            )
        )
        compose.setContent {
            OjoCiudadanoTheme {
                NotificationsContent(
                    state = state.value,
                    notifications = rows.value,
                    onBack = {},
                    onRetry = {},
                    isRefreshing = refreshing.value,
                    onRefresh = { refreshing.value = true }
                )
            }
        }
        val list = compose.onNodeWithTag("notifications_refresh")
        list.performTouchInput { swipeDown() }
        indicator.assertIsDisplayed()
        compose.runOnIdle {
            rows.value = listOf(notification)
            state.value = NotificationsUiState.Success
            refreshing.value = false
        }
        compose.onNodeWithText(notification.message).assertIsDisplayed()
        indicator.assertDoesNotExist()

        list.performTouchInput { swipeDown() }
        indicator.assertIsDisplayed()
        compose.onNodeWithText(notification.message).assertIsDisplayed()
        compose.runOnIdle {
            state.value = NotificationsUiState.Error(
                "Revisa tu conexión.",
                canRetry = true,
                duringRefresh = true
            )
            refreshing.value = false
        }
        compose.onNodeWithText("Revisa tu conexión.").assertIsDisplayed()
        compose.onNodeWithText(notification.message).assertIsDisplayed()
        indicator.assertDoesNotExist()
        list.performTouchInput { swipeDown() }
        indicator.assertIsDisplayed()
    }

    @Test fun unreadCardOffersIndividualMarkAndReadCardDoesNot() {
        val marked = mutableListOf<String>()
        compose.setContent {
            OjoCiudadanoTheme {
                NotificationsContent(
                    state = NotificationsUiState.Success,
                    notifications = listOf(
                        notification(id = "u", isRead = false),
                        notification(id = "r", isRead = true)
                    ),
                    onBack = {},
                    onRetry = {},
                    onMarkAsRead = { marked += it.id }
                )
            }
        }

        compose.onNodeWithTag(NotificationsTestTags.unreadIndicator("u")).assertIsDisplayed()
        compose.onNodeWithTag(NotificationsTestTags.unreadIndicator("r")).assertDoesNotExist()
        compose.onNodeWithTag(NotificationsTestTags.markRead("r")).assertDoesNotExist()

        compose.onNodeWithTag(NotificationsTestTags.markRead("u")).performClick()
        assertEquals(listOf("u"), marked)
    }

    @Test fun markingStateShowsProgressAndDisablesTheCardAction() {
        compose.setContent {
            OjoCiudadanoTheme {
                NotificationsContent(
                    state = NotificationsUiState.Success,
                    notifications = listOf(notification(id = "u", isRead = false)),
                    onBack = {},
                    onRetry = {},
                    markingIds = setOf("u"),
                    onMarkAsRead = {}
                )
            }
        }

        compose.onNodeWithTag(NotificationsTestTags.marking("u")).assertIsDisplayed()
        compose.onNodeWithText("Marcando…").assertIsDisplayed()
        compose.onNodeWithTag(NotificationsTestTags.markRead("u")).assertIsNotEnabled()
    }

    @Test fun markAllActionCallsBackAndDisablesWithoutUnreadRows() {
        var markedAll = 0
        val rows = mutableStateOf(listOf(notification(id = "u", isRead = false)))
        compose.setContent {
            OjoCiudadanoTheme {
                NotificationsContent(
                    state = NotificationsUiState.Success,
                    notifications = rows.value,
                    onBack = {},
                    onRetry = {},
                    unreadCount = rows.value.count { !it.isRead },
                    onMarkAllAsRead = { markedAll++ }
                )
            }
        }

        compose.onNodeWithTag(NotificationsTestTags.MARK_ALL).performClick()
        assertEquals(1, markedAll)

        compose.runOnIdle { rows.value = listOf(notification(id = "r", isRead = true)) }
        compose.onNodeWithTag(NotificationsTestTags.MARK_ALL).assertIsNotEnabled()
    }

    @Test fun markAllInProgressShowsProgressAndBlocksAnotherTap() {
        var markedAll = 0
        compose.setContent {
            OjoCiudadanoTheme {
                NotificationsContent(
                    state = NotificationsUiState.Success,
                    notifications = listOf(notification(id = "u", isRead = false)),
                    onBack = {},
                    onRetry = {},
                    unreadCount = 1,
                    isMarkingAll = true,
                    onMarkAllAsRead = { markedAll++ }
                )
            }
        }

        compose.onNodeWithTag(NotificationsTestTags.MARK_ALL_PROGRESS).assertIsDisplayed()
        compose.onNodeWithTag(NotificationsTestTags.MARK_ALL).assertIsNotEnabled()
        assertEquals(0, markedAll)
    }

    @Test fun actionErrorIsShownDismissableAndSeparateFromTheQueryError() {
        var dismissed = false
        compose.setContent {
            OjoCiudadanoTheme {
                NotificationsContent(
                    state = NotificationsUiState.Success,
                    notifications = listOf(notification(id = "u", isRead = false)),
                    onBack = {},
                    onRetry = {},
                    actionError = "No se pudo marcar la notificación (HTTP 500).",
                    onDismissActionError = { dismissed = true }
                )
            }
        }

        compose.onNodeWithTag(NotificationsTestTags.ACTION_ERROR).assertIsDisplayed()
        compose.onNodeWithText("No se pudo marcar la notificación (HTTP 500).").assertIsDisplayed()
        compose.onNodeWithText("Entendido").performClick()
        assertTrue(dismissed)
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
