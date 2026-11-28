package cl.uct.ojociudadano.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards readability when institutional or semantic tokens change. */
class DesignContrastTest {
    @Test fun textAndSemanticLabelsMeetNormalTextContrast() {
        checkTextContrast(LightColorScheme, IncidentTones.Light)
        checkTextContrast(DarkColorScheme, IncidentTones.Dark)
    }

    private fun checkTextContrast(scheme: androidx.compose.material3.ColorScheme, tones: IncidentTones) {
        val pairs = listOf(
            scheme.primary to scheme.onPrimary,
            scheme.secondary to scheme.onSecondary,
            scheme.tertiary to scheme.onTertiary,
            scheme.error to scheme.onError,
            scheme.background to scheme.onBackground,
            scheme.surface to scheme.onSurface,
            scheme.surfaceVariant to scheme.onSurfaceVariant,
            scheme.surfaceContainerHighest to scheme.onSurfaceVariant,
            scheme.primaryContainer to scheme.onPrimaryContainer,
            scheme.secondaryContainer to scheme.onSecondaryContainer,
            scheme.tertiaryContainer to scheme.onTertiaryContainer,
            scheme.primaryFixed to scheme.onPrimaryFixed,
            scheme.primaryFixedDim to scheme.onPrimaryFixedVariant,
            scheme.secondaryFixed to scheme.onSecondaryFixed,
            scheme.secondaryFixedDim to scheme.onSecondaryFixedVariant,
            scheme.tertiaryFixed to scheme.onTertiaryFixed,
            scheme.tertiaryFixedDim to scheme.onTertiaryFixedVariant,
            scheme.inverseSurface to scheme.inverseOnSurface,
            scheme.inverseSurface to scheme.inversePrimary,
            scheme.errorContainer to scheme.onErrorContainer
        ) + listOf("Nuevo", "En revisión", "En atención", "Resuelto", "Cerrado", "Desconocido", null)
            .map(tones::status).map { it.container to it.content } +
            ((0..6).toList() + null).map(tones::priority).map { it.container to it.content }
        pairs.forEach { (background, foreground) ->
            assertTrue("Insufficient text contrast: $background / $foreground", contrast(background, foreground) >= 4.5f)
        }
    }

    @Test fun fieldOutlinesMeetNonTextContrast() {
        assertTrue(contrast(LightColorScheme.outline, LightColorScheme.surface) >= 3f)
        assertTrue(contrast(DarkColorScheme.outline, DarkColorScheme.surface) >= 3f)
    }

    private fun contrast(first: Color, second: Color): Float {
        val a = first.luminance()
        val b = second.luminance()
        return (maxOf(a, b) + 0.05f) / (minOf(a, b) + 0.05f)
    }
}
