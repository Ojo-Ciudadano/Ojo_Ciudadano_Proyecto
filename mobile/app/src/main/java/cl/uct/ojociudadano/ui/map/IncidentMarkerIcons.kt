package cl.uct.ojociudadano.ui.map

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.toArgb
import cl.uct.ojociudadano.ui.theme.DarkColorScheme
import cl.uct.ojociudadano.ui.theme.IncidentTones
import cl.uct.ojociudadano.ui.theme.LightColorScheme
import kotlin.math.roundToInt

/**
 * Caché en memoria de los íconos ya renderizados para los marcadores del mapa.
 *
 * Las combinaciones de estado, prioridad, selección y tema son finitas. Reutilizar la
 * misma instancia evita crear bitmaps cada vez que osmdroid redibuja la capa de
 * marcadores o el estado de la pantalla se recompone.
 */
internal class IncidentMarkerIconCache(private val resources: Resources) {

    private val icons = HashMap<Pair<IncidentMarkerVisual, Boolean>, BitmapDrawable>()

    /** Cantidad de íconos distintos renderizados; útil para verificar la caché. */
    val cachedIconCount: Int
        get() = icons.size

    /** Devuelve el ícono cacheado para [visual] o lo renderiza la primera vez. */
    fun iconFor(visual: IncidentMarkerVisual, darkTheme: Boolean = false): BitmapDrawable =
        icons.getOrPut(visual to darkTheme) {
            BitmapDrawable(resources, renderMarkerBitmap(visual, darkTheme)).apply {
                isFilterBitmap = true
            }
        }

    private fun renderMarkerBitmap(visual: IncidentMarkerVisual, darkTheme: Boolean): Bitmap {
        val metrics = resources.displayMetrics
        val density = metrics.density
        val width = (if (visual.selected) SELECTED_WIDTH_DP else NORMAL_WIDTH_DP) * density
        val height = (if (visual.selected) SELECTED_HEIGHT_DP else NORMAL_HEIGHT_DP) * density
        val bitmap = Bitmap.createBitmap(
            width.roundToInt().coerceAtLeast(1),
            height.roundToInt().coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        ).apply {
            setDensity(if (metrics.densityDpi > 0) metrics.densityDpi else DEFAULT_DENSITY_DPI)
        }

        val canvas = Canvas(bitmap)
        val padding = (if (visual.selected) SELECTED_PADDING_DP else NORMAL_PADDING_DP) * density
        val maxRing = (if (visual.selected) SELECTED_OUTER_RING_DP else NORMAL_HALO_DP) * density
        val bodyWidth = bitmap.width - 2f * padding
        val radius = bodyWidth / 2f - maxRing / 2f
        val centerX = bitmap.width / 2f
        val circleCenterY = padding + maxRing / 2f + radius
        val tipY = bitmap.height - padding
        val pinPath = pinPath(centerX, circleCenterY, radius, tipY)

        // Sombra sutil para separar el pin en mapas claros.
        canvas.save()
        canvas.translate(0f, density)
        canvas.drawPath(pinPath, fillPaint(Color.argb(45, 0, 0, 0)))
        canvas.restore()

        val tones = if (darkTheme) IncidentTones.Dark else IncidentTones.Light
        val colors = if (darkTheme) DarkColorScheme else LightColorScheme
        val tone = tones.status(visual.status.backendName)
        val fillColor = tone.container.toArgb()
        val contentColor = tone.content.toArgb()

        if (visual.selected) {
            // El pin seleccionado suma un contorno azul institucional y se dibuja
            // más grande para destacar sobre el resto de marcadores.
            canvas.drawPath(
                pinPath,
                strokePaint(colors.primary.toArgb(), SELECTED_OUTER_RING_DP * density)
            )
        }
        canvas.drawPath(
            pinPath,
            strokePaint(
                Color.WHITE,
                (if (visual.selected) SELECTED_HALO_DP else NORMAL_HALO_DP) * density
            )
        )
        canvas.drawPath(
            pinPath,
            strokePaint(
                contentColor,
                (if (visual.selected) SELECTED_OUTLINE_DP else NORMAL_OUTLINE_DP) * density
            )
        )
        canvas.drawPath(pinPath, fillPaint(fillColor))

        // Círculo central neutro: deja el glifo de prioridad legible sin alterar
        // el color que identifica el estado.
        canvas.drawCircle(centerX, circleCenterY, radius * 0.55f, fillPaint(colors.surface.toArgb()))
        val textPaint = fillPaint(contentColor).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            textSize = radius * 0.85f
        }
        canvas.drawText(
            visual.priority.glyph,
            centerX,
            circleCenterY - (textPaint.ascent() + textPaint.descent()) / 2f,
            textPaint
        )

        return bitmap
    }

    private fun pinPath(centerX: Float, circleCenterY: Float, radius: Float, tipY: Float): Path {
        val circle = Path().apply {
            addCircle(centerX, circleCenterY, radius, Path.Direction.CW)
        }
        val tip = Path().apply {
            val baseHalfWidth = radius * 0.62f
            moveTo(centerX - baseHalfWidth, circleCenterY + radius * 0.72f)
            lineTo(centerX, tipY)
            lineTo(centerX + baseHalfWidth, circleCenterY + radius * 0.72f)
            close()
        }
        return Path().apply {
            op(circle, tip, Path.Op.UNION)
        }
    }

    private fun fillPaint(color: Int): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
        isDither = true
    }

    private fun strokePaint(color: Int, strokeWidth: Float): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
        }

    private companion object {
        const val NORMAL_WIDTH_DP = 32f
        const val NORMAL_HEIGHT_DP = 42f
        const val NORMAL_PADDING_DP = 2f
        const val NORMAL_HALO_DP = 4f
        const val NORMAL_OUTLINE_DP = 2f
        const val SELECTED_WIDTH_DP = 42f
        const val SELECTED_HEIGHT_DP = 54f
        const val SELECTED_PADDING_DP = 2.5f
        const val SELECTED_OUTER_RING_DP = 10f
        const val SELECTED_HALO_DP = 7f
        const val SELECTED_OUTLINE_DP = 2.5f
        const val DEFAULT_DENSITY_DPI = 160
    }
}

/**
 * Proveedor del caché compartido por los mapas de la aplicación.
 *
 * Se recrea únicamente si cambia la densidad de pantalla, porque el tamaño en
 * píxeles de los bitmaps depende de ella.
 */
internal object IncidentMarkerIconProvider {

    private var cache: IncidentMarkerIconCache? = null
    private var densityDpi: Int = 0

    @Synchronized
    fun cacheFor(context: Context): IncidentMarkerIconCache {
        val resources = context.applicationContext.resources
        val currentDensity = resources.displayMetrics.densityDpi
        val current = cache
        if (current != null && currentDensity == densityDpi) return current

        return IncidentMarkerIconCache(resources).also {
            cache = it
            densityDpi = currentDensity
        }
    }
}
