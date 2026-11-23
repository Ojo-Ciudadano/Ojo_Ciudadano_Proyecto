package cl.uct.ojociudadano.data.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.exifinterface.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pruebas instrumentadas del compresor real: requieren `Bitmap` y
 * `ExifInterface`, por eso no viven en `test/`.
 */
@RunWith(AndroidJUnit4::class)
class AndroidEvidenceImageCompressorTest {

    private lateinit var directory: File
    private lateinit var compressor: AndroidEvidenceImageCompressor

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        directory = File(context.cacheDir, "evidence-compressor-test").apply { mkdirs() }
        compressor = AndroidEvidenceImageCompressor(
            outputDirectory = directory,
            maxUploadBytes = 300_000L,
            maxDimension = 1_024
        )
    }

    @After
    fun tearDown() {
        directory.deleteRecursively()
    }

    @Test
    fun compressesLargePhotoBelowUploadLimitAndTargetDimension(): Unit = runBlocking {
        val source = writeNoisyPhoto("grande.jpg", width = 3_000, height = 2_000)
        assertTrue("La foto de prueba debe superar el límite", source.length() > 300_000L)

        val compressed = compressor.compress(source, "image/jpeg")

        try {
            assertTrue(compressed.isTemporary)
            assertTrue(
                "El resultado debe quedar bajo el límite: ${compressed.file.length()}",
                compressed.file.length() <= 300_000L
            )
            val (width, height) = readBounds(compressed.file)
            assertTrue(width <= 1_024)
            assertTrue(height <= 1_024)
            assertEquals("image/jpeg", compressed.mimeType)
        } finally {
            compressed.file.delete()
            source.delete()
        }
    }

    @Test
    fun keepsSmallPhotoUntouched(): Unit = runBlocking {
        val source = writePhoto("pequena.jpg", width = 320, height = 240, quality = 80)

        val result = compressor.compress(source, "image/jpeg")

        assertFalse(result.isTemporary)
        assertEquals(source.absolutePath, result.file.absolutePath)
        source.delete()
    }

    @Test
    fun appliesExifOrientationBeforeScaling(): Unit = runBlocking {
        val source = writePhoto("rotada.jpg", width = 2_000, height = 1_000, quality = 100)
        ExifInterface(source.absolutePath).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            saveAttributes()
        }

        val result = compressor.compress(source, "image/jpeg")

        try {
            val (width, height) = readBounds(result.file)
            assertTrue("Debe quedar rotada: ${width}x${height}", width < height)
            assertTrue(height <= 1_024)
        } finally {
            result.file.delete()
            source.delete()
        }
    }

    @Test
    fun rejectsCorruptFile(): Unit = runBlocking {
        val corrupt = File(directory, "corrupta.jpg").apply {
            writeBytes(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8))
        }

        val failure = runCatching { compressor.compress(corrupt, "image/jpeg") }.exceptionOrNull()

        assertTrue(failure is EvidenceImageException)
        assertEquals(EvidenceImageFailure.CORRUPT, (failure as EvidenceImageException).failure)
        corrupt.delete()
    }

    @Test
    fun rejectsUnsupportedMimeType(): Unit = runBlocking {
        val source = writePhoto("formato.jpg", width = 200, height = 200, quality = 80)

        val failure = runCatching { compressor.compress(source, "image/gif") }.exceptionOrNull()

        assertTrue(failure is EvidenceImageException)
        assertEquals(EvidenceImageFailure.UNSUPPORTED_FORMAT, (failure as EvidenceImageException).failure)
        source.delete()
    }

    @Test
    fun rejectsMissingFile(): Unit = runBlocking {
        val missing = File(directory, "no-existe.jpg")

        val failure = runCatching { compressor.compress(missing, "image/jpeg") }.exceptionOrNull()

        assertTrue(failure is EvidenceImageException)
        assertEquals(EvidenceImageFailure.UNAVAILABLE, (failure as EvidenceImageException).failure)
    }

    private fun writePhoto(name: String, width: Int, height: Int, quality: Int): File {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(Color.rgb(40, 90, 160))
        val paint = android.graphics.Paint().apply { color = Color.rgb(220, 180, 40) }
        canvas.drawCircle(width / 2f, height / 2f, minOf(width, height) / 3f, paint)
        val file = File(directory, name)
        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
        }
        bitmap.recycle()
        assertNotNull(file)
        return file
    }

    /**
     * Genera una fotografía con ruido determinista: la textura aleatoria hace
     * que el JPEG pese más de 300 KB incluso con la compresión de calidad 100.
     */
    private fun writeNoisyPhoto(name: String, width: Int, height: Int): File {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val random = kotlin.random.Random(7L)
        val pixels = IntArray(width * height) {
            Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256))
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        val file = File(directory, name)
        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)
        }
        bitmap.recycle()
        return file
    }

    private fun readBounds(file: File): Pair<Int, Int> {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        return options.outWidth to options.outHeight
    }
}
