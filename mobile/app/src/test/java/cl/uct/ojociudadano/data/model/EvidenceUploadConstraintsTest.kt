package cl.uct.ojociudadano.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EvidenceUploadConstraintsTest {

    @Test
    fun `accepts supported mime types without case sensitivity`() {
        listOf("image/jpeg", "image/png", "image/webp", "IMAGE/JPEG").forEach { mimeType ->
            assertTrue(mimeType, EvidenceUploadConstraints.isSupportedMimeType(mimeType))
        }
    }

    @Test
    fun `rejects mime types that the backend does not accept`() {
        listOf("image/heic", "image/heif", "image/gif", "image/bmp", "").forEach { mimeType ->
            assertFalse(mimeType, EvidenceUploadConstraints.isSupportedMimeType(mimeType))
        }
    }

    @Test
    fun `accepts a photo exactly at the size limit`() {
        val photo = samplePhoto.copy(sizeBytes = EvidenceUploadConstraints.MAX_FILE_SIZE_BYTES)

        assertTrue(
            EvidenceUploadConstraints.issuesFor(photo, fileAvailable = true).isEmpty()
        )
    }

    @Test
    fun `rejects a photo above the size limit`() {
        val photo = samplePhoto.copy(
            sizeBytes = EvidenceUploadConstraints.MAX_FILE_SIZE_BYTES + 1
        )

        assertEquals(
            listOf(EvidenceCompatibilityIssue.FILE_TOO_LARGE),
            EvidenceUploadConstraints.issuesFor(photo, fileAvailable = true)
        )
    }

    @Test
    fun `reports empty and unavailable files`() {
        val emptyPhoto = samplePhoto.copy(sizeBytes = 0L)

        assertEquals(
            listOf(EvidenceCompatibilityIssue.EMPTY_FILE),
            EvidenceUploadConstraints.issuesFor(emptyPhoto, fileAvailable = true)
        )
        assertEquals(
            listOf(EvidenceCompatibilityIssue.FILE_UNAVAILABLE),
            EvidenceUploadConstraints.issuesFor(samplePhoto, fileAvailable = false)
        )
    }

    @Test
    fun `reports every issue that applies to the same photo`() {
        val photo = samplePhoto.copy(
            mimeType = "image/heic",
            sizeBytes = EvidenceUploadConstraints.MAX_FILE_SIZE_BYTES + 1
        )

        assertEquals(
            listOf(
                EvidenceCompatibilityIssue.FILE_UNAVAILABLE,
                EvidenceCompatibilityIssue.UNSUPPORTED_FORMAT,
                EvidenceCompatibilityIssue.FILE_TOO_LARGE
            ),
            EvidenceUploadConstraints.issuesFor(photo, fileAvailable = false)
        )
    }

    private companion object {
        val samplePhoto = IncidentPhotoEvidence(
            localUri = "content://evidences/evidence.jpg",
            fileName = "evidence.jpg",
            mimeType = "image/jpeg",
            sizeBytes = 1024L,
            capturedAt = 1_700_000_000_000L
        )
    }
}
