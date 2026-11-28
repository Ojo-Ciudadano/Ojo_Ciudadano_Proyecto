package cl.uct.ojociudadano.data.preferences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeModeTest {
    @Test fun systemFollowsBothAndroidModes() {
        assertFalse(ThemeMode.SYSTEM.isDark(systemIsDark = false))
        assertTrue(ThemeMode.SYSTEM.isDark(systemIsDark = true))
    }

    @Test fun manualModesOverrideBothAndroidModes() {
        for (systemIsDark in listOf(false, true)) {
            assertFalse(ThemeMode.LIGHT.isDark(systemIsDark))
            assertTrue(ThemeMode.DARK.isDark(systemIsDark))
        }
    }

    @Test fun missingAndUnknownValuesFallBackToSystem() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStoredValue(null))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStoredValue("future-theme"))
    }
}
