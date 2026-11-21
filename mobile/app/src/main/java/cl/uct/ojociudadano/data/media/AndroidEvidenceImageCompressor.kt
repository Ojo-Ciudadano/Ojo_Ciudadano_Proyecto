package cl.uct.ojociudadano.data.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import cl.uct.ojociudadano.data.model.EvidenceUploadConstraints
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Prepara fotografías para la subida: las decodifica sin desbordar memoria,
 * corrige la orientación EXIF y las recomprime hasta quedar bajo el límite.
 *
 * Toda la operación ocurre en [ioDispatcher] y el archivo resultante es
 * temporal: quien lo consume debe borrarlo (la capa de datos lo hace en
 * `finally`).
 */
class AndroidEvidenceImageCompressor(
    private val outputDirectory: File,
    private val maxUploadBytes: Long = EvidenceUploadConstraints.MAX_FILE_SIZE_BYTES,
    private val maxDimension: Int = ImageCompressionPolicy.MAX_DIMENSION,
    private val clock: () -> Long = System::currentTimeMillis,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : EvidenceImageCompressor {

    override suspend fun compress(file: File, mimeType: String): CompressedEvidence =
        withContext(ioDispatcher) {
            val normalizedMime = EvidenceMultipartFactory.normalizedMimeType(mimeType)

            if (!file.isFile || !file.canRead() || file.length() <= 0L) {
                throw EvidenceImageException(
                    EvidenceImageFailure.UNAVAILABLE,
                    "La fotografía ya no está disponible en el dispositivo"
                )
            }
            if (EvidenceMultipartFactory.extensionFor(normalizedMime) == null) {
                throw EvidenceImageException(
                    EvidenceImageFailure.UNSUPPORTED_FORMAT,
                    "Formato de archivo no soportado por el backend. Use JPEG, PNG o WebP"
                )
            }

            val bounds = readBounds(file) ?: throw EvidenceImageException(
                EvidenceImageFailure.CORRUPT,
                "La fotografía está dañada o no es una imagen válida"
            )

            if (!ImageCompressionPolicy.shouldCompress(
                    file.length(),
                    bounds.first,
                    bounds.second
                )
            ) {
                return@withContext CompressedEvidence(
                    file = file,
                    mimeType = normalizedMime,
                    isTemporary = false
                )
            }

            val decoded = decodeSampled(file, bounds.first, bounds.second)
                ?: throw EvidenceImageException(
                    EvidenceImageFailure.CORRUPT,
                    "La fotografía está dañada o no es una imagen válida"
                )

            var working = decoded
            try {
                currentCoroutineContext().ensureActive()
                working = applyExifOrientation(file, working)
                working = scaleDown(working)
                working = flattenOverWhite(working)

                val output = writeUnderLimit(working)
                CompressedEvidence(
                    file = output,
                    mimeType = JPEG_MIME_TYPE,
                    isTemporary = true
                )
            } finally {
                if (!working.isRecycled) working.recycle()
                if (decoded !== working && !decoded.isRecycled) decoded.recycle()
            }
        }

    private fun readBounds(file: File): Pair<Int, Int>? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) return null
        return options.outWidth to options.outHeight
    }

    private fun decodeSampled(file: File, width: Int, height: Int): Bitmap? {
        val sampleSize = ImageCompressionPolicy.computeInSampleSize(
            width = width,
            height = height,
            maxDimension = maxDimension
        )
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        return try {
            BitmapFactory.decodeFile(file.absolutePath, options)
        } catch (_: OutOfMemoryError) {
            options.inPreferredConfig = Bitmap.Config.RGB_565
            options.inSampleSize = sampleSize * 2
            runCatching { BitmapFactory.decodeFile(file.absolutePath, options) }.getOrNull()
        }
    }

    /**
     * Aplica la orientación EXIF de la cámara y devuelve el `Bitmap` ya
     * normalizado. El bitmap recibido se recicla solo cuando se crea otro.
     */
    private fun applyExifOrientation(file: File, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            ExifInterface(file).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }

        return runCatching {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }.getOrDefault(bitmap).also { rotated ->
            if (rotated !== bitmap) bitmap.recycle()
        }
    }

    private fun scaleDown(bitmap: Bitmap): Bitmap {
        val (targetWidth, targetHeight) = ImageCompressionPolicy.scaledDimensions(
            bitmap.width,
            bitmap.height,
            maxDimension
        )
        if (targetWidth == bitmap.width && targetHeight == bitmap.height) return bitmap

        val scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        if (scaled !== bitmap) bitmap.recycle()
        return scaled
    }

    /**
     * JPEG no conserva transparencia: se aplana sobre blanco para que las
     * zonas translúcidas no queden negras.
     */
    private fun flattenOverWhite(bitmap: Bitmap): Bitmap {
        if (!bitmap.hasAlpha()) return bitmap

        val flattened = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(flattened)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        bitmap.recycle()
        return flattened
    }

    private suspend fun writeUnderLimit(bitmap: Bitmap): File {
        ensureOutputDirectory()

        var current = bitmap
        var ownsCurrent = false
        var pass = 0

        while (true) {
            currentCoroutineContext().ensureActive()

            for (quality in ImageCompressionPolicy.QUALITY_STEPS) {
                currentCoroutineContext().ensureActive()

                val candidate = newOutputFile()
                writeJpeg(current, quality, candidate)
                if (candidate.length() in 1..maxUploadBytes) {
                    if (ownsCurrent) current.recycle()
                    return candidate
                }
                candidate.delete()
            }

            if (pass >= ImageCompressionPolicy.MAX_PASSES) break

            val (targetWidth, targetHeight) =
                ImageCompressionPolicy.downscale(current.width, current.height)
            if (targetWidth == current.width && targetHeight == current.height) break

            val smaller = Bitmap.createScaledBitmap(current, targetWidth, targetHeight, true)
            if (smaller === current) break
            if (ownsCurrent) current.recycle()
            current = smaller
            ownsCurrent = true
            pass++
        }

        if (ownsCurrent) current.recycle()
        throw EvidenceImageException(
            EvidenceImageFailure.CANNOT_FIT,
            "No fue posible reducir la fotografía por debajo de " +
                "${EvidenceUploadConstraints.MAX_FILE_SIZE_MB} MB"
        )
    }

    private fun writeJpeg(bitmap: Bitmap, quality: Int, destination: File) {
        FileOutputStream(destination).use { output ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)) {
                throw EvidenceImageException(
                    EvidenceImageFailure.CORRUPT,
                    "No fue posible comprimir la fotografía"
                )
            }
        }
    }

    private fun ensureOutputDirectory() {
        if (!outputDirectory.isDirectory && !outputDirectory.mkdirs()) {
            throw EvidenceImageException(
                EvidenceImageFailure.UNAVAILABLE,
                "No fue posible preparar el espacio temporal para la evidencia"
            )
        }
    }

    private fun newOutputFile(): File =
        File(outputDirectory, "evidence_upload_${clock()}_${UUID.randomUUID()}.jpg")

    private companion object {
        const val JPEG_MIME_TYPE = "image/jpeg"
    }
}
