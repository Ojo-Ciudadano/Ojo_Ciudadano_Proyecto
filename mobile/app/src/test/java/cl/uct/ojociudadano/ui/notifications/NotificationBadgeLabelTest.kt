package cl.uct.ojociudadano.ui.notifications

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationBadgeLabelTest {
    @Test
    fun hidesTheBadgeWithoutUnreadNotifications() {
        assertEquals("", unreadBadgeLabel(0))
        assertEquals("", unreadBadgeLabel(-3))
    }

    @Test
    fun showsTheExactCountUpToNinetyNine() {
        assertEquals("1", unreadBadgeLabel(1))
        assertEquals("42", unreadBadgeLabel(42))
        assertEquals("99", unreadBadgeLabel(99))
    }

    @Test
    fun capsTheLabelAboveNinetyNine() {
        assertEquals("99+", unreadBadgeLabel(100))
        assertEquals("99+", unreadBadgeLabel(1_500))
    }
}
