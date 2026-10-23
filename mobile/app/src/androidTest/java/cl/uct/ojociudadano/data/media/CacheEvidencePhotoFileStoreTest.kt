package cl.uct.ojociudadano.data.media

import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CacheEvidencePhotoFileStoreTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val store = CacheEvidencePhotoFileStore(context)
    private val createdFileNames = mutableListOf<String>()

    @After
    fun tearDown() {
        createdFileNames.forEach(store::delete)
    }

    @Test
    fun reservesCacheFileExposedThroughFileProvider() {
        val pending = store.create()
        createdFileNames += pending.fileName

        assertTrue(pending.fileName.endsWith(".jpg"))
        assertTrue(pending.localUri.startsWith("content://"))
        assertTrue(pending.localUri.contains("fileprovider"))
        assertTrue(fileFor(pending.fileName).exists())
    }

    @Test
    fun completesEvidenceReadingContentWrittenByCamera() {
        val pending = store.create()
        createdFileNames += pending.fileName

        context.contentResolver.openOutputStream(Uri.parse(pending.localUri))!!.use { output ->
            output.write(byteArrayOf(1, 2, 3, 4))
        }

        val evidence = requireNotNull(store.complete(pending))

        assertEquals(pending.localUri, evidence.localUri)
        assertEquals(pending.fileName, evidence.fileName)
        assertEquals("image/jpeg", evidence.mimeType)
        assertEquals(4L, evidence.sizeBytes)
        assertTrue(evidence.capturedAt > 0L)
    }

    @Test
    fun returnsNullWhenCameraLeavesTheReservedFileEmpty() {
        val pending = store.create()
        createdFileNames += pending.fileName

        assertNull(store.complete(pending))
    }

    @Test
    fun deletesTemporaryFileByName() {
        val pending = store.create()

        store.delete(pending.fileName)

        assertNull(store.complete(pending))
    }

    @Test
    fun importsImageFromContentUriIntoTheCache() {
        val source = store.create()
        createdFileNames += source.fileName
        val content = validImageBytes(Bitmap.CompressFormat.JPEG)

        context.contentResolver.openOutputStream(Uri.parse(source.localUri))!!.use { output ->
            output.write(content)
        }

        val imported = runBlocking { store.import(source.localUri) }
        createdFileNames += imported.fileName

        assertNotEquals(source.fileName, imported.fileName)
        assertTrue(imported.localUri.startsWith("content://"))
        assertTrue(imported.localUri.contains("fileprovider"))
        assertTrue(imported.fileName.endsWith(".jpg"))
        assertEquals("image/jpeg", imported.mimeType)
        assertEquals(content.size.toLong(), imported.sizeBytes)
        assertTrue(imported.capturedAt > 0L)
        assertTrue(fileFor(imported.fileName).exists())
        assertArrayEquals(content, fileFor(imported.fileName).readBytes())
        assertTrue(fileFor(source.fileName).exists())
    }

    @Test
    fun preservesTheMimeTypeOfAnImportedPngImage() {
        val content = validImageBytes(Bitmap.CompressFormat.PNG)
        val sourceFile = temporaryFile(extension = "png", content = content)
        val sourceUri = FileProvider.getUriForFile(context, authority, sourceFile)

        try {
            val imported = runBlocking { store.import(sourceUri.toString()) }
            createdFileNames += imported.fileName

            assertEquals("image/png", imported.mimeType)
            assertTrue(imported.fileName.endsWith(".png"))
            assertEquals(content.size.toLong(), imported.sizeBytes)
            assertArrayEquals(content, fileFor(imported.fileName).readBytes())
        } finally {
            sourceFile.delete()
        }
    }

    @Test
    fun usesTheDetectedMimeTypeWhenTheExtensionIsMisleading() {
        val content = validImageBytes(Bitmap.CompressFormat.PNG)
        val sourceFile = temporaryFile(extension = "jpg", content = content)
        val sourceUri = FileProvider.getUriForFile(context, authority, sourceFile)

        try {
            val imported = runBlocking { store.import(sourceUri.toString()) }
            createdFileNames += imported.fileName

            assertEquals("image/png", imported.mimeType)
            assertTrue(imported.fileName.endsWith(".png"))
            assertArrayEquals(content, fileFor(imported.fileName).readBytes())
        } finally {
            sourceFile.delete()
        }
    }

    @Test
    fun rejectsImageBytesWithAnImageExtensionButInvalidContent() {
        val sourceFile = temporaryFile(extension = "jpg", content = "no es una imagen")
        val sourceUri = FileProvider.getUriForFile(context, authority, sourceFile)
        val filesBefore = evidenceFileNames()

        try {
            runBlocking { store.import(sourceUri.toString()) }
            fail("Se esperaba PhotoImportException")
        } catch (exception: PhotoImportException) {
            assertEquals(PhotoImportFailure.NOT_AN_IMAGE, exception.failure)
            assertEquals(filesBefore, evidenceFileNames())
        } finally {
            sourceFile.delete()
        }
    }

    @Test
    fun rejectsACorruptedImageWithAValidHeaderAndRemovesThePartialFile() {
        val corruptedPng = pngHeader() + "contenido incompleto".toByteArray()
        val sourceFile = temporaryFile(extension = "png", content = corruptedPng)
        val sourceUri = FileProvider.getUriForFile(context, authority, sourceFile)
        val filesBefore = evidenceFileNames()

        try {
            runBlocking { store.import(sourceUri.toString()) }
            fail("Se esperaba PhotoImportException")
        } catch (exception: PhotoImportException) {
            assertEquals(PhotoImportFailure.INVALID_IMAGE, exception.failure)
            assertEquals(filesBefore, evidenceFileNames())
        } finally {
            sourceFile.delete()
        }
    }

    @Test
    fun rejectsContentThatIsNotAnImage() {
        val sourceFile = temporaryFile(extension = "txt", content = "no es una imagen")
        val sourceUri = FileProvider.getUriForFile(context, authority, sourceFile)

        try {
            runBlocking { store.import(sourceUri.toString()) }
            fail("Se esperaba PhotoImportException")
        } catch (exception: PhotoImportException) {
            assertEquals(PhotoImportFailure.NOT_AN_IMAGE, exception.failure)
        } finally {
            sourceFile.delete()
        }
    }

    @Test
    fun rejectsAnEmptyImageAndRemovesThePartialFile() {
        val sourceFile = temporaryFile(extension = "jpg")
        val sourceUri = FileProvider.getUriForFile(context, authority, sourceFile)
        val filesBefore = evidenceFileNames()

        try {
            runBlocking { store.import(sourceUri.toString()) }
            fail("Se esperaba PhotoImportException")
        } catch (exception: PhotoImportException) {
            assertEquals(PhotoImportFailure.EMPTY_FILE, exception.failure)
            assertEquals(filesBefore, evidenceFileNames())
        } finally {
            sourceFile.delete()
        }
    }

    @Test
    fun rejectsAnImageWhoseDeclaredSizeExceedsTheLimit() {
        val sourceFile = temporaryFile(extension = "jpg", content = ByteArray(64))
        val sourceUri = FileProvider.getUriForFile(context, authority, sourceFile)
        val limitedStore = CacheEvidencePhotoFileStore(context, maxImportBytes = 16L)
        val filesBefore = evidenceFileNames()

        try {
            runBlocking { limitedStore.import(sourceUri.toString()) }
            fail("Se esperaba PhotoImportException")
        } catch (exception: PhotoImportException) {
            assertEquals(PhotoImportFailure.FILE_TOO_LARGE, exception.failure)
            assertEquals(filesBefore, evidenceFileNames())
        } finally {
            sourceFile.delete()
        }
    }

    @Test
    fun rejectsAnImageWithoutDeclaredSizeWhenCopiedContentExceedsTheLimit() {
        val sourceUri = Uri.parse("content://${UnknownSizeImageProvider.AUTHORITY}/image")
        val limitedStore = CacheEvidencePhotoFileStore(context, maxImportBytes = 16L)
        val filesBefore = evidenceFileNames()

        try {
            runBlocking { limitedStore.import(sourceUri.toString()) }
            fail("Se esperaba PhotoImportException")
        } catch (exception: PhotoImportException) {
            assertEquals(PhotoImportFailure.FILE_TOO_LARGE, exception.failure)
            assertEquals(filesBefore, evidenceFileNames())
        }
    }

    @Test
    fun reportsUnreadableContentWhenTheSourceCannotBeOpened() {
        val source = store.create()
        val contentUri = source.localUri

        context.contentResolver.openOutputStream(Uri.parse(contentUri))!!.use { output ->
            output.write(byteArrayOf(1, 2, 3))
        }
        store.delete(source.fileName)
        val filesBefore = evidenceFileNames()

        try {
            runBlocking { store.import(contentUri) }
            fail("Se esperaba PhotoImportException")
        } catch (exception: PhotoImportException) {
            assertEquals(PhotoImportFailure.UNREADABLE, exception.failure)
            assertEquals(filesBefore, evidenceFileNames())
        }
    }

    private fun validImageBytes(format: Bitmap.CompressFormat): ByteArray =
        ByteArrayOutputStream().use { output ->
            val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)

            try {
                assertTrue(bitmap.compress(format, 100, output))
            } finally {
                bitmap.recycle()
            }

            output.toByteArray()
        }

    private fun pngHeader(): ByteArray = byteArrayOf(
        0x89.toByte(),
        0x50,
        0x4E,
        0x47,
        0x0D,
        0x0A,
        0x1A,
        0x0A
    )

    private fun temporaryFile(extension: String, content: String = ""): File =
        temporaryFile(extension = extension, content = content.toByteArray())

    private fun temporaryFile(extension: String, content: ByteArray): File {
        val directory = File(context.cacheDir, CacheEvidencePhotoFileStore.EVIDENCE_DIRECTORY_NAME)
        directory.mkdirs()

        return File(directory, "import_test_${System.nanoTime()}.$extension").apply {
            writeBytes(content)
        }
    }

    private fun evidenceFileNames(): List<String> =
        File(context.cacheDir, CacheEvidencePhotoFileStore.EVIDENCE_DIRECTORY_NAME)
            .listFiles()
            ?.map(File::getName)
            ?.sorted()
            .orEmpty()

    private fun fileFor(fileName: String): File = File(
        File(context.cacheDir, CacheEvidencePhotoFileStore.EVIDENCE_DIRECTORY_NAME),
        fileName
    )

    private val authority: String
        get() = "${context.packageName}.fileprovider"
}
