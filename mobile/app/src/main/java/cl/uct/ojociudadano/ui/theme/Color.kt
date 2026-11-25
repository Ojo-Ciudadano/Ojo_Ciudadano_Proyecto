package cl.uct.ojociudadano.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

val CitizenRed = Color(0xFFB42332)
val CitizenBlue = Color(0xFF102A43)
val CitizenBackground = Color(0xFFF4F6FA)

internal val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFB5CBE3),
    secondary = Color(0xFFFFB3B8),
    background = Color(0xFF101820),
    surface = Color(0xFF18232E)
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

object IncidentTones {
    val information = IncidentTone(Color(0xFFE0EAF4), CitizenBlue)
    val review = IncidentTone(Color(0xFFFFF0C2), Color(0xFF634600))
    val attention = IncidentTone(Color(0xFFECE4FF), Color(0xFF493078))
    val resolved = IncidentTone(Color(0xFFD7F3E3), Color(0xFF145533))
    val closed = IncidentTone(Color(0xFFE3E8EE), Color(0xFF354558))
    val critical = IncidentTone(Color(0xFFFFE4E6), Color(0xFF68131D))
    val high = IncidentTone(Color(0xFFFFE6D5), Color(0xFF783609))
}

fun incidentStatusTone(name: String?): IncidentTone = when (name?.trim()?.lowercase(java.util.Locale.ROOT)) {
    "nuevo" -> IncidentTones.information
    "en revisión" -> IncidentTones.review
    "en atención" -> IncidentTones.attention
    "resuelto" -> IncidentTones.resolved
    "cerrado" -> IncidentTones.closed
    else -> IncidentTones.closed
}

// The API defines levels 1 (minimum) through 5 (critical).
fun incidentPriorityTone(level: Int?): IncidentTone = when (level) {
    1 -> IncidentTones.information
    2 -> IncidentTones.resolved
    3 -> IncidentTones.review
    4 -> IncidentTones.high
    5 -> IncidentTones.critical
    else -> IncidentTones.closed
}
