package cl.uct.ojociudadano.ui.map

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import cl.uct.ojociudadano.ui.theme.IncidentTone
import cl.uct.ojociudadano.ui.theme.IncidentTones
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.TilesOverlay
import kotlin.math.roundToInt

internal fun MapView.applyMapAppearance(darkTheme: Boolean, background: Int) {
    setBackgroundColor(background)
    overlayManager.tilesOverlay.apply {
        loadingBackgroundColor = Color.TRANSPARENT
        setColorFilter(if (darkTheme) TilesOverlay.INVERT_COLORS else null)
    }
    invalidate()
}

/** Reutiliza el bitmap por estado y prioridad dentro de la vista del mapa. */
internal class IncidentMarkerIcons(
    private val resources: Resources,
    private val tones: IncidentTones
) {
    private val icons = mutableMapOf<Pair<IncidentTone, Int?>, Bitmap>()

    fun bitmap(summary: IncidentMapSummary): Bitmap {
        val tone = tones.status(summary.statusName)
        val priority = summary.priorityLevel?.takeIf { it in 1..5 }
        return icons.getOrPut(tone to priority) { drawPin(tone, priority) }
    }

    fun drawable(bitmap: Bitmap): BitmapDrawable = bitmap.toDrawable(resources)

    private fun drawPin(tone: IncidentTone, priority: Int?): Bitmap {
        val density = resources.displayMetrics.density
        val bitmap = createBitmap(
            (36 * density).roundToInt(), (44 * density).roundToInt()
        ).apply { this.density = resources.displayMetrics.densityDpi }
        val canvas = Canvas(bitmap)
        canvas.scale(density, density)
        val pin = Path().apply {
            moveTo(18f, 41f)
            cubicTo(15f, 36f, 3f, 27f, 3f, 18f)
            cubicTo(3f, -2f, 33f, -2f, 33f, 18f)
            cubicTo(33f, 27f, 21f, 36f, 18f, 41f)
            close()
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeJoin = Paint.Join.ROUND
            style = Paint.Style.STROKE
            strokeWidth = 5f
            color = Color.BLACK
        }
        // Los dos bordes mantienen el contorno visible sobre teselas claras y oscuras.
        canvas.drawPath(pin, paint)
        paint.strokeWidth = 3f
        paint.color = Color.WHITE
        canvas.drawPath(pin, paint)
        paint.style = Paint.Style.FILL
        paint.color = tone.container.toArgb()
        canvas.drawPath(pin, paint)
        paint.color = tone.content.toArgb()
        if (priority == null) {
            canvas.drawCircle(18f, 17f, 3f, paint)
        } else {
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = 17f
            canvas.drawText(priority.toString(), 18f, 23f, paint)
        }
        return bitmap
    }
}
