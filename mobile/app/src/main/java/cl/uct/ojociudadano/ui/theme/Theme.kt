package cl.uct.ojociudadano.ui.theme

import androidx.compose.ui.unit.dp
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFFFFB3B8),
    secondary = androidx.compose.ui.graphics.Color(0xFFB5CBE3),
    background = androidx.compose.ui.graphics.Color(0xFF101820),
    surface = androidx.compose.ui.graphics.Color(0xFF18232E)
)

private val LightColorScheme = lightColorScheme(
    primary = CitizenRed,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFFFE4E6),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF68131D),
    secondary = CitizenBlue,
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFFE0EAF4),
    onSecondaryContainer = CitizenBlue,
    background = CitizenBackground,
    surface = androidx.compose.ui.graphics.Color.White,
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFE8EDF3),
    onSurface = CitizenBlue,
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF45566A)
)

@Composable
fun OjoCiudadanoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = androidx.compose.material3.Shapes(
            small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            large = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
        ),
        content = content
    )
}