package cl.uct.ojociudadano.data.preferences

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThemePreferencesTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val preferences = context.getSharedPreferences("appearance-test", Context.MODE_PRIVATE)

    @After fun clearTestPreferences() {
        context.deleteSharedPreferences("appearance-test")
    }

    @Test fun selectionUpdatesImmediatelyAndIsReadByTheNextInstance() {
        val store = ThemePreferences(preferences)
        assertEquals(ThemeMode.SYSTEM, store.mode.value)
        for (mode in listOf(ThemeMode.DARK, ThemeMode.LIGHT, ThemeMode.SYSTEM)) {
            instrumentation.runOnMainSync { store.setMode(mode) }
            assertEquals(mode, store.mode.value)
            assertEquals(mode, ThemePreferences(preferences).mode.value)
        }
    }

    @Test fun unknownStoredValuesUseSystem() {
        preferences.edit().putString("theme_mode", "obsolete").commit()
        assertEquals(ThemeMode.SYSTEM, ThemePreferences(preferences).mode.value)
        preferences.edit().putInt("theme_mode", 6).commit()
        assertEquals(ThemeMode.SYSTEM, ThemePreferences(preferences).mode.value)
    }
}
