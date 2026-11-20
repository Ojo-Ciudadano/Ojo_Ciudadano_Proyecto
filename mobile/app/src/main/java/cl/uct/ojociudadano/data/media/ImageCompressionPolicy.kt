package cl.uct.ojociudadano.data.media

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Reglas puras de redimensionado y compresión de evidencias.
 *
 * Vive separada de `Bitmap` para poder probarse en la JVM sin Android y para
 * que la implementación instrumentada solo se ocupe de decodificar, rotar y
 * escribir archivos.
 */
object ImageCompressionPolicy {

    /** Lado mayor de la fotografía ya procesada. */
    const val MAX_DIMENSION = 2_048

    /** Por debajo de este tamaño no vale la pena recomprimir. */
    const val COMPRESS_THRESHOLD_BYTES = 1L * 1024L * 1024L

    /** Escalera de calidades JPEG; se prueba de mayor a menor. */
    val QUALITY_STEPS = intArrayOf(85, 75, 65, 55, 45, 35)

    /** Factor de reducción cuando ninguna calidad alcanza el límite. */
    const val DOWNSCALE_FACTOR = 0.85f

    /** Pasadas máximas de reducción para no degradar indefinidamente. */
    const val MAX_PASSES = 3

    /**
     * Calcula el `inSampleSize` (potencia de dos) para decodificar sin
     * desbordar memoria.
     *
     * Se elige el mayor factor que deje el lado mayor en o por debajo del
     * objetivo; así el `Bitmap` decodificado ronda [maxDimension] píxeles y la
     * escala final apenas pierde detalle sin cargar la imagen completa.
     */
    fun computeInSampleSize(
        width: Int,
        height: Int,
        maxDimension: Int = MAX_DIMENSION
    ): Int {
        if (width <= 0 || height <= 0 || maxDimension <= 0) return 1

        val largest = max(width, height)
        var sampleSize = 1
        while (largest / sampleSize > maxDimension) {
            sampleSize *= 2
        }
        // Si la imagen ya cabe, el bucle no entra y se decodifica tal cual.
        return sampleSize
    }

    /**
     * Dimensiones finales conservando la relación de aspecto.
     *
     * Nunca amplía una imagen: si ya cabe, devuelve el mismo tamaño.
     */
    fun scaledDimensions(
        width: Int,
        height: Int,
        maxDimension: Int = MAX_DIMENSION
    ): Pair<Int, Int> {
        if (width <= 0 || height <= 0) return width to height

        val largest = max(width, height)
        if (largest <= maxDimension) return width to height

        val scale = maxDimension.toFloat() / largest
        return max(1, (width * scale).roundToInt()) to
            max(1, (height * scale).roundToInt())
    }

    /** Indica si merece la pena recomprimir un archivo del tamaño dado. */
    fun shouldCompress(sizeBytes: Long, width: Int, height: Int): Boolean =
        sizeBytes > COMPRESS_THRESHOLD_BYTES ||
            max(width, height) > MAX_DIMENSION

    /** Aplica [DOWNSCALE_FACTOR] sobre unas dimensiones ya escaladas. */
    fun downscale(width: Int, height: Int): Pair<Int, Int> {
        if (width <= 1 || height <= 1) return width to height
        return max(1, (width * DOWNSCALE_FACTOR).roundToInt()) to
            max(1, (height * DOWNSCALE_FACTOR).roundToInt())
    }
}
