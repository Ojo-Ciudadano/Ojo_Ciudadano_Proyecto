package cl.uct.ojociudadano.data.media

import android.net.Uri
import androidx.core.content.FileProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
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
        val content = byteArrayOf(1, 2, 3, 4)

        context.contentResolver.openOutputStream(Uri.parse(source.localUri))!!.use { output ->
            output.write(content)
        }

        val imported = store.import(source.localUri)
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
    fun rejectsContentThatIsNotAnImage() {
        val sourceFile = temporaryFile(extension = "txt", content = "no es una imagen")
        val sourceUri = FileProvider.getUriForFile(context, authority, sourceFile)

        try {
            store.import(sourceUri.toString())
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
            store.import(sourceUri.toString())
            fail("Se esperaba PhotoImportException")
        } catch (exception: PhotoImportException) {
            assertEquals(PhotoImportFailure.EMPTY_FILE, exception.failure)
            assertEquals(filesBefore, evidenceFileNames())
        } finally {
            sourceFile.delete()
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
            store.import(contentUri)
            fail("Se esperaba PhotoImportException")
        } catch (exception: PhotoImportException) {
            assertEquals(PhotoImportFailure.UNREADABLE, exception.failure)
            assertEquals(filesBefore, evidenceFileNames())
        }
    }

    private fun temporaryFile(extension: String, content: String = ""): File {
        val directory = File(context.cacheDir, CacheEvidencePhotoFileStore.EVIDENCE_DIRECTORY_NAME)
        directory.mkdirs()

        return File(directory, "import_test_${System.nanoTime()}.$extension").apply {
            writeText(content)
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
