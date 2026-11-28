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
import cl.uct.ojociudadano.ui.theme.CitizenBlue
import cl.uct.ojociudadano.ui.theme.incidentStatusTone
import kotlin.math.roundToInt

/**
 * Caché en memoria de los íconos ya renderizados para los marcadores del mapa.
 *
 * Las combinaciones de estado, prioridad y selección son finitas; reutilizar la
 * misma instancia evita crear bitmaps cada vez que osmdroid redibuja la capa de
 * marcadores o el estado de la pantalla se recompone.
 */
internal class IncidentMarkerIconCache(private val resources: Resources) {

    private val icons = HashMap<IncidentMarkerVisual, BitmapDrawable>()

    /** Cantidad de íconos distintos renderizados; útil para verificar la caché. */
    val cachedIconCount: Int
        get() = icons.size

    /** Devuelve el ícono cacheado para [visual] o lo renderiza la primera vez. */
    fun iconFor(visual: IncidentMarkerVisual): BitmapDrawable =
        icons.getOrPut(visual) {
            BitmapDrawable(resources, renderMarkerBitmap(visual)).apply {
                isFilterBitmap = true
            }
        }

    private fun renderMarkerBitmap(visual: IncidentMarkerVisual): Bitmap {
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

        // Se usan los mismos pares container/content que los badges de estado:
        // el relleno claro identifica el estado y el borde/glifo oscuro garantiza
        // legibilidad sobre el mapa.
        val tone = incidentStatusTone(visual.status.backendName)
        val fillColor = tone.container.toArgb()
        val contentColor = tone.content.toArgb()

        if (visual.selected) {
            // El pin seleccionado suma un contorno azul institucional y se dibuja
            // más grande para destacar sobre el resto de marcadores.
            canvas.drawPath(
                pinPath,
                strokePaint(CitizenBlue.toArgb(), SELECTED_OUTER_RING_DP * density)
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
        canvas.drawCircle(centerX, circleCenterY, radius * 0.55f, fillPaint(Color.WHITE))
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
 * Caché de los íconos que representan un clúster de incidentes.
 *
 * El contador es la única dimensión variable del ícono, por lo que basta con
 * una entrada por cantidad distinta. El texto se abrevia a `999+` para que
 * grupos muy numerosos sigan siendo legibles.
 */
internal class IncidentClusterIconCache(private val resources: Resources) {

    private val icons = HashMap<Int, BitmapDrawable>()

    /** Cantidad de íconos distintos renderizados; útil para verificar la caché. */
    val cachedIconCount: Int
        get() = icons.size

    /** Devuelve el ícono cacheado para [count] o lo renderiza la primera vez. */
    fun iconFor(count: Int): BitmapDrawable =
        icons.getOrPut(count) {
            BitmapDrawable(resources, renderClusterBitmap(count)).apply {
                isFilterBitmap = true
            }
        }

    private fun renderClusterBitmap(count: Int): Bitmap {
        val density = resources.displayMetrics.density
        val size = (CLUSTER_SIZE_DP * density).roundToInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
            setDensity(
                resources.displayMetrics.densityDpi.takeIf { it > 0 }
                    ?: DEFAULT_DENSITY_DPI
            )
        }

        val canvas = Canvas(bitmap)
        val center = size / 2f
        val radius = center - CLUSTER_PADDING_DP * density

        // Sombra sutil para separar el clúster de mapas con colores claros.
        canvas.drawCircle(
            center,
            center + density,
            radius,
            fillPaint(Color.argb(45, 0, 0, 0))
        )
        canvas.drawCircle(center, center, radius, fillPaint(Color.WHITE))
        canvas.drawCircle(
            center,
            center,
            radius - CLUSTER_BORDER_DP * density,
            fillPaint(CitizenBlue.toArgb())
        )

        val label = if (count > MAX_DISPLAYED_COUNT) {
            "$MAX_DISPLAYED_COUNT+"
        } else {
            count.toString()
        }
        val textPaint = fillPaint(Color.WHITE).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            textSize = when (label.length) {
                1, 2 -> radius * 0.95f
                3 -> radius * 0.72f
                else -> radius * 0.56f
            }
        }
        canvas.drawText(
            label,
            center,
            center - (textPaint.ascent() + textPaint.descent()) / 2f,
            textPaint
        )

        return bitmap
    }

    private fun fillPaint(color: Int): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
        isDither = true
    }

    private companion object {
        const val CLUSTER_SIZE_DP = 46f
        const val CLUSTER_PADDING_DP = 2f
        const val CLUSTER_BORDER_DP = 2.5f
        const val MAX_DISPLAYED_COUNT = 999
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
    private var clusterCache: IncidentClusterIconCache? = null
    private var densityDpi: Int = 0
    private var clusterDensityDpi: Int = 0

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

    @Synchronized
    fun clusterCacheFor(context: Context): IncidentClusterIconCache {
        val resources = context.applicationContext.resources
        val currentDensity = resources.displayMetrics.densityDpi
        val current = clusterCache
        if (current != null && currentDensity == clusterDensityDpi) return current

        return IncidentClusterIconCache(resources).also {
            clusterCache = it
            clusterDensityDpi = currentDensity
        }
    }
}
