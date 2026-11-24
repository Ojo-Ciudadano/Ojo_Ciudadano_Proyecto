package cl.uct.ojociudadano.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards readability when institutional or semantic tokens change. */
class DesignContrastTest {
    @Test fun textAndSemanticLabelsMeetNormalTextContrast() {
        val scheme = LightColorScheme
        val pairs = listOf(
            scheme.primary to scheme.onPrimary,
            scheme.secondary to scheme.onSecondary,
            scheme.background to scheme.onBackground,
            scheme.surface to scheme.onSurface,
            scheme.surfaceContainerHighest to scheme.onSurfaceVariant,
            scheme.primaryContainer to scheme.onPrimaryContainer,
            scheme.secondaryContainer to scheme.onSecondaryContainer,
            scheme.errorContainer to scheme.onErrorContainer
        ) + listOf("Nuevo", "En revisión", "En atención", "Resuelto", "Cerrado", "Desconocido")
            .map { incidentStatusTone(it).let { tone -> tone.container to tone.content } } +
            (0..6).map { incidentPriorityTone(it).let { tone -> tone.container to tone.content } }
        pairs.forEach { (background, foreground) ->
            assertTrue("Insufficient text contrast: $background / $foreground", contrast(background, foreground) >= 4.5f)
        }
    }

    @Test fun fieldOutlinesMeetNonTextContrast() {
        assertTrue(contrast(LightColorScheme.outline, LightColorScheme.surface) >= 3f)
    }

    private fun contrast(first: Color, second: Color): Float {
        val a = first.luminance()
        val b = second.luminance()
        return (maxOf(a, b) + 0.05f) / (minOf(a, b) + 0.05f)
    }
}
