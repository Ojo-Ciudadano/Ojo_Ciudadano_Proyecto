package cl.uct.ojociudadano.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import java.util.Locale

val CitizenRed = Color(0xFFB42332)
val CitizenBlue = Color(0xFF102A43)
val CitizenBackground = Color(0xFFF4F6FA)

internal val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFB5CBE3),
    onPrimary = CitizenBlue,
    primaryContainer = Color(0xFF28445E),
    onPrimaryContainer = Color(0xFFE0EAF4),
    inversePrimary = CitizenBlue,
    secondary = Color(0xFFFFB3B8),
    onSecondary = Color(0xFF550D1A),
    secondaryContainer = Color(0xFF68131D),
    onSecondaryContainer = Color(0xFFFFDADD),
    tertiary = Color(0xFFD9C5FF),
    onTertiary = Color(0xFF372264),
    tertiaryContainer = Color(0xFF493078),
    onTertiaryContainer = Color(0xFFECE4FF),
    background = Color(0xFF101820),
    onBackground = Color(0xFFE7EDF5),
    surface = Color(0xFF18232E),
    onSurface = Color(0xFFE7EDF5),
    surfaceVariant = Color(0xFF2A3949),
    onSurfaceVariant = Color(0xFFBCCBDC),
    surfaceDim = Color(0xFF101820),
    surfaceBright = Color(0xFF38495C),
    surfaceContainerLowest = Color(0xFF0D141C),
    surfaceContainerLow = Color(0xFF1A2632),
    surfaceContainer = Color(0xFF202D3A),
    surfaceContainerHigh = Color(0xFF273544),
    surfaceContainerHighest = Color(0xFF304052),
    surfaceTint = Color(0xFFB5CBE3),
    inverseSurface = Color(0xFFE7EDF5),
    inverseOnSurface = CitizenBlue,
    outline = Color(0xFF899CB1),
    outlineVariant = Color(0xFF4B5F74),
    error = Color(0xFFFFB3B8),
    onError = Color(0xFF550D1A),
    errorContainer = Color(0xFF68131D),
    onErrorContainer = Color(0xFFFFDADD),
    scrim = Color.Black
)

internal val LightColorScheme = lightColorScheme(
    primary = CitizenBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0EAF4),
    onPrimaryContainer = CitizenBlue,
    secondary = CitizenRed,
    secondaryContainer = Color(0xFFFFE4E6),
    onSecondaryContainer = Color(0xFF68131D),
    onSecondary = Color.White,
    error = CitizenRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFE4E6),
    onErrorContainer = Color(0xFF68131D),
    background = CitizenBackground,
    onBackground = CitizenBlue,
    surface = Color.White,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFF0F3F8),
    surfaceContainerHigh = Color(0xFFEAF0F6),
    surfaceContainerHighest = Color(0xFFE2E9F1),
    surfaceTint = CitizenBlue,
    outline = Color(0xFF718096),
    outlineVariant = Color(0xFFD7E0EA),
    surfaceVariant = Color(0xFFE8EDF3),
    onSurface = CitizenBlue,
    onSurfaceVariant = Color(0xFF45566A)
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
