package cl.uct.ojociudadano.ui.report

import cl.uct.ojociudadano.data.model.IncidentPhotoEvidence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateReportExitPolicyTest {

    @Test
    fun `partial upload failure requires confirmation for pending evidence`() {
        val risk = CreateReportExitPolicy.riskOf(
            state = failedState(EvidenceUploadFailureKind.CONFIRMED),
            visiblePhotos = emptyList()
        )

        assertEquals(
            CreateReportExitRisk(
                pendingEvidenceCount = 1,
                incidentAlreadyCreated = true,
                isUploading = false
            ),
            risk
        )
        assertTrue(
            CreateReportExitPolicy.message(requireNotNull(risk))
                .contains("queda 1 fotografía sin subir")
        )
    }

    @Test
    fun `pending evidence remains a risk even when the thumbnail is not visible`() {
        val risk = CreateReportExitPolicy.riskOf(
            state = failedState(EvidenceUploadFailureKind.POSSIBLY_UPLOADED),
            visiblePhotos = emptyList()
        )

        assertNotNull(risk)
        assertEquals(1, risk?.pendingEvidenceCount)
        assertTrue(risk?.incidentAlreadyCreated == true)
    }

    @Test
    fun `failed state without pending evidence does not block navigation`() {
        val risk = CreateReportExitPolicy.riskOf(
            state = CreateReportSubmissionState.EvidenceUploadFailed(
                incidentId = INCIDENT_ID,
                uploadedEvidenceCount = 2,
                pendingEvidence = emptyList(),
                message = ""
            ),
            visiblePhotos = listOf(photo)
        )

        assertNull(risk)
    }

    @Test
    fun `uploading evidence requires confirmation and reports the full total`() {
        val risk = CreateReportExitPolicy.riskOf(
            state = CreateReportSubmissionState.UploadingEvidence(
                incidentId = INCIDENT_ID,
                processed = 1,
                total = 3
            ),
            visiblePhotos = emptyList()
        )

        assertEquals(
            CreateReportExitRisk(
                pendingEvidenceCount = 3,
                incidentAlreadyCreated = true,
                isUploading = true
            ),
            risk
        )

        val message = CreateReportExitPolicy.message(requireNotNull(risk))
        assertTrue(message.contains("Quedan 3 fotografías pendientes de subir"))
        assertTrue(message.contains("cancelará la subida"))
    }

    @Test
    fun `uploading state without evidence does not require confirmation`() {
        val risk = CreateReportExitPolicy.riskOf(
            state = CreateReportSubmissionState.UploadingEvidence(
                incidentId = INCIDENT_ID,
                processed = 0,
                total = 0
            ),
            visiblePhotos = listOf(photo)
        )

        assertNull(risk)
    }

    @Test
    fun `creating the incident with photos requires confirmation`() {
        val risk = CreateReportExitPolicy.riskOf(
            state = CreateReportSubmissionState.CreatingIncident,
            visiblePhotos = listOf(photo, secondPhoto)
        )

        assertEquals(
            CreateReportExitRisk(
                pendingEvidenceCount = 2,
                incidentAlreadyCreated = false,
                isUploading = false
            ),
            risk
        )
        assertTrue(
            CreateReportExitPolicy.message(requireNotNull(risk))
                .contains("se está creando con 2 fotografías")
        )
    }

    @Test
    fun `creating the incident without photos does not require confirmation`() {
        val risk = CreateReportExitPolicy.riskOf(
            state = CreateReportSubmissionState.CreatingIncident,
            visiblePhotos = emptyList()
        )

        assertNull(risk)
    }

    @Test
    fun `completed or failed creation does not block navigation`() {
        assertNull(
            CreateReportExitPolicy.riskOf(
                state = CreateReportSubmissionState.Success(
                    incidentId = INCIDENT_ID,
                    uploadedEvidenceCount = 2
                ),
                visiblePhotos = listOf(photo)
            )
        )
        assertNull(
            CreateReportExitPolicy.riskOf(
                state = CreateReportSubmissionState.Error("No fue posible crear el reporte"),
                visiblePhotos = listOf(photo)
            )
        )
    }

    @Test
    fun `idle form with selected photos does not require confirmation`() {
        assertNull(
            CreateReportExitPolicy.riskOf(
                state = CreateReportSubmissionState.Idle,
                visiblePhotos = listOf(photo)
            )
        )
    }

    @Test
    fun `messages use singular when only one photo is involved`() {
        val uploadingRisk = CreateReportExitPolicy.riskOf(
            state = CreateReportSubmissionState.UploadingEvidence(
                incidentId = INCIDENT_ID,
                processed = 0,
                total = 1
            ),
            visiblePhotos = emptyList()
        )
        val creatingRisk = CreateReportExitPolicy.riskOf(
            state = CreateReportSubmissionState.CreatingIncident,
            visiblePhotos = listOf(photo)
        )

        assertTrue(
            CreateReportExitPolicy.message(requireNotNull(uploadingRisk))
                .contains("Queda 1 fotografía pendiente de subir")
        )
        assertTrue(
            CreateReportExitPolicy.message(requireNotNull(creatingRisk))
                .contains("con 1 fotografía")
        )
    }

    @Test
    fun `failed state uses only the pending registry instead of visible photos`() {
        val risk = CreateReportExitPolicy.riskOf(
            state = failedState(EvidenceUploadFailureKind.CONFIRMED),
            visiblePhotos = listOf(photo, secondPhoto, thirdPhoto)
        )

        assertEquals(1, risk?.pendingEvidenceCount)
        assertFalse(risk?.isUploading == true)
    }

    private fun failedState(
        failureKind: EvidenceUploadFailureKind
    ): CreateReportSubmissionState.EvidenceUploadFailed =
        CreateReportSubmissionState.EvidenceUploadFailed(
            incidentId = INCIDENT_ID,
            uploadedEvidenceCount = 1,
            pendingEvidence = listOf(
                PendingEvidenceUpload(
                    photo = photo,
                    failureKind = failureKind
                )
            ),
            message = EvidenceUploadErrors.partial(failed = 1, total = 2)
        )

    private companion object {
        const val INCIDENT_ID = "incident-id"

        val photo = IncidentPhotoEvidence(
            localUri = "content://evidences/evidence_1.jpg",
            fileName = "evidence_1.jpg",
            mimeType = "image/jpeg",
            sizeBytes = 1024L,
            capturedAt = 1_700_000_000_000L
        )

        val secondPhoto = photo.copy(
            localUri = "content://evidences/evidence_2.jpg",
            fileName = "evidence_2.jpg"
        )

        val thirdPhoto = photo.copy(
            localUri = "content://evidences/evidence_3.jpg",
            fileName = "evidence_3.jpg"
        )
    }
}
