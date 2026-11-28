package cl.uct.ojociudadano.data.media

import java.io.File
import java.nio.file.Files
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EvidenceTempCleanerTest {

    private val root: File = Files.createTempDirectory("evidence-temp-cleaner").toFile()
    private val evidences = File(root, "evidences")
    private val uploads = File(root, "evidence-upload")

    @After
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun `removes expired files from both directories and keeps the recent ones`() {
        val now = 1_700_000_000_000L
        val oldEvidence = createFile(evidences, "old.jpg", lastModified = now - 48 * 60 * 60 * 1000L)
        val recentEvidence = createFile(evidences, "recent.jpg", lastModified = now - 60_000L)
        val oldUpload = createFile(uploads, "old-upload.jpg", lastModified = now - 48 * 60 * 60 * 1000L)
        val recentUpload = createFile(uploads, "recent-upload.jpg", lastModified = now - 60_000L)
        val cleaner = EvidenceTempCleaner(evidences, uploads) { now }

        val deleted = cleaner.cleanupExpired(maxAgeMillis = 24 * 60 * 60 * 1000L)

        assertEquals(2, deleted)
        assertFalse(oldEvidence.exists())
        assertFalse(oldUpload.exists())
        assertTrue(recentEvidence.exists())
        assertTrue(recentUpload.exists())
    }

    @Test
    fun `purges every file regardless of its age`() {
        val evidence = createFile(evidences, "evidence.jpg")
        val upload = createFile(uploads, "upload.jpg")
        val cleaner = EvidenceTempCleaner(evidences, uploads)

        val deleted = cleaner.purgeAll()

        assertEquals(2, deleted)
        assertFalse(evidence.exists())
        assertFalse(upload.exists())
    }

    @Test
    fun `tolerates missing directories`() {
        val cleaner = EvidenceTempCleaner(
            File(root, "missing-evidences"),
            File(root, "missing-uploads")
        )

        assertEquals(0, cleaner.cleanupExpired())
        assertEquals(0, cleaner.purgeAll())
    }

    private fun createFile(
        directory: File,
        name: String,
        lastModified: Long = System.currentTimeMillis()
    ): File {
        assertTrue(directory.mkdirs() || directory.isDirectory)
        return File(directory, name).apply {
            writeBytes(byteArrayOf(1, 2, 3))
            setLastModified(lastModified)
        }
    }
}
