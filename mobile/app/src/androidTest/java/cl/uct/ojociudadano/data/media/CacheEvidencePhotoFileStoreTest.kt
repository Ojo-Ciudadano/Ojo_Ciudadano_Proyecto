package cl.uct.ojociudadano.data.media

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    private fun fileFor(fileName: String): File = File(
        File(context.cacheDir, CacheEvidencePhotoFileStore.EVIDENCE_DIRECTORY_NAME),
        fileName
    )
}
