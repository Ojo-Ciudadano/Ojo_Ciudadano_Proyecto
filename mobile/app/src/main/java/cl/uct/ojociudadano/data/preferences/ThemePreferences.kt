package cl.uct.ojociudadano.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.MainThread
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(internal val storedValue: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    fun isDark(systemIsDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemIsDark
        LIGHT -> false
        DARK -> true
    }

    internal companion object {
        fun fromStoredValue(value: String?): ThemeMode =
            entries.firstOrNull { it.storedValue == value } ?: SYSTEM
    }
}

/** Preferencia del dispositivo, compartida por todas las sesiones. */
class ThemePreferences internal constructor(private val preferences: SharedPreferences) {
    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences("appearance", Context.MODE_PRIVATE)
    )

    private val currentMode = MutableStateFlow(
        ThemeMode.fromStoredValue(preferences.all[KEY] as? String)
    )
    val mode = currentMode.asStateFlow()

    @MainThread
    fun setMode(mode: ThemeMode) {
        preferences.edit { putString(KEY, mode.storedValue) }
        currentMode.value = mode
    }

    private companion object {
        const val KEY = "theme_mode"
    }
}
