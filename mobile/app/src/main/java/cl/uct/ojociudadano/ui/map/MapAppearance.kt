package cl.uct.ojociudadano.ui.map

import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import org.osmdroid.views.MapView

internal fun MapView.applyMapAppearance(darkTheme: Boolean, background: Int) {
    setBackgroundColor(background)
    clipToOutline = true
    overlayManager.tilesOverlay.apply {
        loadingBackgroundColor = Color.TRANSPARENT
        setColorFilter(if (darkTheme) darkTileFilter else null)
    }
    invalidate()
}

// Invierte la luminancia de las teselas raster y atenúa los colores del mapa base.
private val darkTileFilter = ColorMatrixColorFilter(ColorMatrix().apply {
    setSaturation(0.35f)
    postConcat(ColorMatrix(floatArrayOf(
        -0.83f, 0f, 0f, 0f, 226f,
        0f, -0.83f, 0f, 0f, 234f,
        0f, 0f, -0.83f, 0f, 246f,
        0f, 0f, 0f, 1f, 0f
    )))
})
