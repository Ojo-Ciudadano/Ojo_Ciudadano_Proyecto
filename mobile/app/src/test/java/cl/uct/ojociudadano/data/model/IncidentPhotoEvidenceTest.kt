package cl.uct.ojociudadano.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class IncidentPhotoEvidenceTest {

    @Test
    fun `uses jpeg as the default mime type for camera captures`() {
        val evidence = IncidentPhotoEvidence(
            localUri = "content://cl.uct.ojociudadano.fileprovider/evidence_photos/evidence_1.jpg",
            fileName = "evidence_1.jpg",
            sizeBytes = 1024L,
            capturedAt = 1_700_000_000_000L
        )

        assertEquals("image/jpeg", evidence.mimeType)
    }
}
