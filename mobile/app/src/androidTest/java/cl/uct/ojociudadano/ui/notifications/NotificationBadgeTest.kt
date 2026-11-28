package cl.uct.ojociudadano.ui.notifications

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import cl.uct.ojociudadano.ui.theme.OjoCiudadanoTheme
import org.junit.Rule
import org.junit.Test

class NotificationBadgeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun showsThePendingCountOverTheIcon() {
        show(count = 3)
        compose.onNodeWithText("3").assertIsDisplayed()
        compose.onNodeWithContentDescription("Notificaciones sin leer: 3").assertIsDisplayed()
    }

    @Test
    fun capsTheBadgeAtNinetyNinePlus() {
        show(count = 120)
        compose.onNodeWithText("99+").assertIsDisplayed()
        compose.onNodeWithContentDescription("Notificaciones sin leer: 120").assertIsDisplayed()
    }

    @Test
    fun hidesTheBadgeWithoutUnreadNotifications() {
        show(count = 0)
        compose.onNodeWithTag(NotificationsTestTags.BADGE).assertDoesNotExist()
        compose.onNodeWithContentDescription("Notificaciones").assertIsDisplayed()
    }

    private fun show(count: Int) {
        compose.setContent {
            OjoCiudadanoTheme {
                NotificationBadge(count = count) {
                    Icon(Icons.Filled.Notifications, contentDescription = "Notificaciones")
                }
            }
        }
    }
}
