package cl.uct.ojociudadano.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import java.util.Locale

val CitizenRed = Color(0xFFB42332)
val CitizenBlue = Color(0xFF102A43)
val CitizenBackground = Color(0xFFF4F6FA)

private val PaleBlue = Color(0xFFE0EAF4)
private val SoftBlue = Color(0xFFB5CBE3)
private val DeepBlue = Color(0xFF28445E)
private val PaleRed = Color(0xFFFFDADD)
private val SoftRed = Color(0xFFFFB3B8)
private val DeepRed = Color(0xFF68131D)
private val PaleViolet = Color(0xFFECE4FF)
private val SoftViolet = Color(0xFFD9C5FF)
private val DeepViolet = Color(0xFF493078)
private val NightBackground = Color(0xFF101820)
private val NightForeground = Color(0xFFE7EDF5)

internal val DarkColorScheme = darkColorScheme(
    primary = SoftBlue,
    onPrimary = CitizenBlue,
    primaryContainer = DeepBlue,
    onPrimaryContainer = PaleBlue,
    primaryFixed = PaleBlue,
    primaryFixedDim = SoftBlue,
    onPrimaryFixed = CitizenBlue,
    onPrimaryFixedVariant = DeepBlue,
    inversePrimary = CitizenBlue,
    secondary = SoftRed,
    onSecondary = Color(0xFF550D1A),
    secondaryContainer = DeepRed,
    onSecondaryContainer = PaleRed,
    secondaryFixed = PaleRed,
    secondaryFixedDim = SoftRed,
    onSecondaryFixed = Color(0xFF550D1A),
    onSecondaryFixedVariant = DeepRed,
    tertiary = SoftViolet,
    onTertiary = Color(0xFF372264),
    tertiaryContainer = DeepViolet,
    onTertiaryContainer = PaleViolet,
    tertiaryFixed = PaleViolet,
    tertiaryFixedDim = SoftViolet,
    onTertiaryFixed = Color(0xFF372264),
    onTertiaryFixedVariant = DeepViolet,
    background = NightBackground,
    onBackground = NightForeground,
    surface = Color(0xFF18232E),
    onSurface = NightForeground,
    surfaceVariant = Color(0xFF2A3949),
    onSurfaceVariant = Color(0xFFBCCBDC),
    surfaceDim = NightBackground,
    surfaceBright = Color(0xFF38495C),
    surfaceContainerLowest = Color(0xFF0D141C),
    surfaceContainerLow = Color(0xFF1A2632),
    surfaceContainer = Color(0xFF202D3A),
    surfaceContainerHigh = Color(0xFF273544),
    surfaceContainerHighest = Color(0xFF304052),
    surfaceTint = SoftBlue,
    inverseSurface = NightForeground,
    inverseOnSurface = CitizenBlue,
    outline = Color(0xFF899CB1),
    outlineVariant = Color(0xFF4B5F74),
    error = SoftRed,
    onError = Color(0xFF550D1A),
    errorContainer = DeepRed,
    onErrorContainer = PaleRed,
    scrim = Color.Black
)

internal val LightColorScheme = lightColorScheme(
    primary = CitizenBlue,
    onPrimary = Color.White,
    primaryContainer = PaleBlue,
    onPrimaryContainer = CitizenBlue,
    primaryFixed = PaleBlue,
    primaryFixedDim = SoftBlue,
    onPrimaryFixed = CitizenBlue,
    onPrimaryFixedVariant = DeepBlue,
    inversePrimary = SoftBlue,
    secondary = CitizenRed,
    secondaryContainer = Color(0xFFFFE4E6),
    onSecondaryContainer = DeepRed,
    onSecondary = Color.White,
    secondaryFixed = PaleRed,
    secondaryFixedDim = SoftRed,
    onSecondaryFixed = Color(0xFF550D1A),
    onSecondaryFixedVariant = DeepRed,
    tertiary = DeepViolet,
    onTertiary = Color.White,
    tertiaryContainer = PaleViolet,
    onTertiaryContainer = DeepViolet,
    tertiaryFixed = PaleViolet,
    tertiaryFixedDim = SoftViolet,
    onTertiaryFixed = Color(0xFF372264),
    onTertiaryFixedVariant = DeepViolet,
    error = CitizenRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFE4E6),
    onErrorContainer = DeepRed,
    background = CitizenBackground,
    onBackground = CitizenBlue,
    surface = Color.White,
    surfaceDim = Color(0xFFD7E0EA),
    surfaceBright = Color.White,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFF0F3F8),
    surfaceContainerHigh = Color(0xFFEAF0F6),
    surfaceContainerHighest = Color(0xFFE2E9F1),
    surfaceTint = CitizenBlue,
    inverseSurface = NightBackground,
    inverseOnSurface = NightForeground,
    outline = Color(0xFF718096),
    outlineVariant = Color(0xFFD7E0EA),
    surfaceVariant = Color(0xFFE8EDF3),
    onSurface = CitizenBlue,
    onSurfaceVariant = Color(0xFF45566A),
    scrim = Color.Black
)

// Fixed media canvas: photos must stay legible independently of the app surface.
val EvidenceCanvas = Color.Black
val OnEvidenceCanvas = Color.White
val EvidenceScrim = EvidenceCanvas.copy(alpha = 0.70f)

/** A foreground/container pair; never use a semantic color without its matching foreground. */
data class IncidentTone(val container: Color, val content: Color)

internal class IncidentTones private constructor(
    private val information: IncidentTone,
    private val review: IncidentTone,
    private val attention: IncidentTone,
    private val resolved: IncidentTone,
    private val closed: IncidentTone,
    private val critical: IncidentTone,
    private val high: IncidentTone
) {
    fun status(name: String?): IncidentTone = when (name?.trim()?.lowercase(Locale.ROOT)) {
        "nuevo" -> information
        "en revisión" -> review
        "en atención" -> attention
        "resuelto" -> resolved
        else -> closed
    }

    // La API define niveles desde 1 (mínima) hasta 5 (crítica).
    fun priority(level: Int?): IncidentTone = when (level) {
        1 -> information
        2 -> resolved
        3 -> review
        4 -> high
        5 -> critical
        else -> closed
    }

    companion object {
        val Light = IncidentTones(
            information = IncidentTone(Color(0xFFE0EAF4), CitizenBlue),
            review = IncidentTone(Color(0xFFFFF0C2), Color(0xFF634600)),
            attention = IncidentTone(Color(0xFFECE4FF), Color(0xFF493078)),
            resolved = IncidentTone(Color(0xFFD7F3E3), Color(0xFF145533)),
            closed = IncidentTone(Color(0xFFE3E8EE), Color(0xFF354558)),
            critical = IncidentTone(Color(0xFFFFE4E6), Color(0xFF68131D)),
            high = IncidentTone(Color(0xFFFFE6D5), Color(0xFF783609))
        )
        val Dark = IncidentTones(
            information = IncidentTone(Color(0xFF28445E), Color(0xFFD7E8FD)),
            review = IncidentTone(Color(0xFF513B08), Color(0xFFFFDF91)),
            attention = IncidentTone(Color(0xFF493078), Color(0xFFECE4FF)),
            resolved = IncidentTone(Color(0xFF194831), Color(0xFFAFF0C9)),
            closed = IncidentTone(Color(0xFF354558), Color(0xFFE1EAF4)),
            critical = IncidentTone(Color(0xFF68131D), Color(0xFFFFDADD)),
            high = IncidentTone(Color(0xFF653315), Color(0xFFFFD3B3))
        )

        @Composable
        fun current(): IncidentTones = if (LocalDarkTheme.current) Dark else Light
    }
}

@Composable
fun incidentStatusTone(name: String?): IncidentTone = IncidentTones.current().status(name)

@Composable
fun incidentPriorityTone(level: Int?): IncidentTone = IncidentTones.current().priority(level)
