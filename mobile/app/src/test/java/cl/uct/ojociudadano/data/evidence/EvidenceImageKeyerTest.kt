package cl.uct.ojociudadano.data.evidence

import cl.uct.ojociudadano.domain.model.Evidence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class EvidenceImageKeyerTest {

    @Test
    fun `uses a stable key based on the evidence id`() {
        assertEquals("evidence:evidencia-1", EvidenceImageKeyer.cacheKey(sampleEvidence()))
    }

    @Test
    fun `different evidences do not share the same cache key`() {
        assertNotEquals(
            EvidenceImageKeyer.cacheKey(sampleEvidence()),
            EvidenceImageKeyer.cacheKey(sampleEvidence().copy(id = "evidencia-2"))
        )
    }

    private fun sampleEvidence() = Evidence(
        id = "evidencia-1",
        incidentId = "incidente-1",
        mimeType = "image/jpeg",
        size = 6L,
        createdAt = "2026-11-02T12:00:00.000Z",
        fileUrl = "/api/v1/evidencias/evidencia-1/archivo"
    )
}
