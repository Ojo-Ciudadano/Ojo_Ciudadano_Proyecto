package cl.uct.ojociudadano.data.media

import org.junit.Assert.assertEquals
import org.junit.Test

class PendingPhotoCaptureTest {

    @Test
    fun `builds evidence with the metadata of the captured file`() {
        val pending = PendingPhotoCapture(
            localUri = "content://cl.uct.ojociudadano.fileprovider/evidence_photos/evidence_1.jpg",
            fileName = "evidence_1.jpg",
            mimeType = "image/jpeg"
        )

        val evidence = pending.toEvidence(
            sizeBytes = 2048L,
            capturedAt = 1_700_000_000_000L
        )

        assertEquals(pending.localUri, evidence.localUri)
        assertEquals(pending.fileName, evidence.fileName)
        assertEquals("image/jpeg", evidence.mimeType)
        assertEquals(2048L, evidence.sizeBytes)
        assertEquals(1_700_000_000_000L, evidence.capturedAt)
    }
}
