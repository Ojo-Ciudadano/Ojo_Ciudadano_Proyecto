package cl.uct.ojociudadano.ui.report.photo

import cl.uct.ojociudadano.data.media.EvidencePhotoFileStore
import cl.uct.ojociudadano.data.media.PendingPhotoCapture
import cl.uct.ojociudadano.data.model.IncidentPhotoEvidence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoCaptureOutcomeTest {

    @Test
    fun `treats a missing pending capture as a cancellation`() {
        val store = FakeEvidencePhotoFileStore()

        val outcome = store.resolveCaptureOutcome(pending = null, captured = false)

        assertEquals(PhotoCaptureOutcome.Cancelled, outcome)
        assertTrue(store.deletedFileNames.isEmpty())
    }

    @Test
    fun `treats a false result as a cancellation and deletes the temporary file`() {
        val store = FakeEvidencePhotoFileStore()

        val outcome = store.resolveCaptureOutcome(pending = pendingCapture, captured = false)

        assertEquals(PhotoCaptureOutcome.Cancelled, outcome)
        assertEquals(listOf(pendingCapture.fileName), store.deletedFileNames)
    }

    @Test
    fun `fails and deletes the file when the camera did not write content`() {
        val store = FakeEvidencePhotoFileStore(completed = null)

        val outcome = store.resolveCaptureOutcome(pending = pendingCapture, captured = true)

        assertEquals(
            PhotoCaptureOutcome.Failed(PhotoCaptureErrors.RECOVER_PHOTO_FAILED),
            outcome
        )
        assertEquals(listOf(pendingCapture.fileName), store.deletedFileNames)
    }

    @Test
    fun `returns the evidence and keeps the file when the capture succeeds`() {
        val store = FakeEvidencePhotoFileStore(completed = evidence)

        val outcome = store.resolveCaptureOutcome(pending = pendingCapture, captured = true)

        assertEquals(PhotoCaptureOutcome.Captured(evidence), outcome)
        assertTrue(store.deletedFileNames.isEmpty())
    }
}

private class FakeEvidencePhotoFileStore(
    private val completed: IncidentPhotoEvidence? = evidence
) : EvidencePhotoFileStore {

    val deletedFileNames = mutableListOf<String>()

    override fun create(): PendingPhotoCapture = pendingCapture

    override fun complete(pending: PendingPhotoCapture): IncidentPhotoEvidence? = completed

    override suspend fun import(contentUri: String): IncidentPhotoEvidence =
        requireNotNull(completed) { "No hay una evidencia importada configurada" }

    override fun delete(fileName: String) {
        deletedFileNames += fileName
    }

    override fun cleanupExpired(maxAgeMillis: Long): Int = 0
}

private val pendingCapture = PendingPhotoCapture(
    localUri = "content://cl.uct.ojociudadano.fileprovider/evidence_photo.jpg",
    fileName = "evidence_photo.jpg",
    mimeType = "image/jpeg"
)

private val evidence = IncidentPhotoEvidence(
    localUri = pendingCapture.localUri,
    fileName = pendingCapture.fileName,
    mimeType = pendingCapture.mimeType,
    sizeBytes = 1024L,
    capturedAt = 1_000L
)
