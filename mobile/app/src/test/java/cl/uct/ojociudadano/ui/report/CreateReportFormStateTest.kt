package cl.uct.ojociudadano.ui.report

import cl.uct.ojociudadano.data.model.Category
import cl.uct.ojociudadano.data.model.IncidentPhotoEvidence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateReportFormStateTest {

    @Test
    fun `updating title clears only its previous error`() {
        val state = CreateReportFormState(
            title = "",
            description = "",
            titleError = "El título es obligatorio",
            descriptionError = "La descripción es obligatoria",
            categoryError = "La categoría es obligatoria"
        )

        val updatedState = state.updateTitle("Poste dañado")

        assertEquals("Poste dañado", updatedState.title)
        assertNull(updatedState.titleError)
        assertEquals(state.description, updatedState.description)
        assertEquals(state.descriptionError, updatedState.descriptionError)
        assertEquals(state.categoryError, updatedState.categoryError)
    }

    @Test
    fun `updating description clears only its previous error`() {
        val state = CreateReportFormState(
            title = "Poste dañado",
            description = "",
            titleError = "El título es obligatorio",
            descriptionError = "La descripción es obligatoria",
            categoryError = "La categoría es obligatoria"
        )

        val updatedState = state.updateDescription("El poste bloquea la vereda")

        assertEquals("El poste bloquea la vereda", updatedState.description)
        assertNull(updatedState.descriptionError)
        assertEquals(state.title, updatedState.title)
        assertEquals(state.titleError, updatedState.titleError)
        assertEquals(state.categoryError, updatedState.categoryError)
    }

    @Test
    fun `updating category stores selection and clears only its previous error`() {
        val category = sampleCategory
        val state = CreateReportFormState(
            title = "Poste dañado",
            description = "El poste bloquea la vereda",
            titleError = "El título es obligatorio",
            descriptionError = "La descripción es obligatoria",
            categoryError = "La categoría es obligatoria"
        )

        val updatedState = state.updateCategory(category)

        assertEquals(category, updatedState.selectedCategory)
        assertNull(updatedState.categoryError)
        assertEquals(state.titleError, updatedState.titleError)
        assertEquals(state.descriptionError, updatedState.descriptionError)
    }

    @Test
    fun `starts without evidence or photo capture errors`() {
        val state = CreateReportFormState()

        assertNull(state.evidencePhoto)
        assertFalse(state.isCapturingPhoto)
        assertNull(state.photoCaptureError)
    }

    @Test
    fun `starting a capture clears the previous error and keeps the current photo`() {
        val state = CreateReportFormState(
            evidencePhoto = samplePhoto,
            isCapturingPhoto = false,
            photoCaptureError = "No fue posible abrir la cámara"
        )

        val updatedState = state.startPhotoCapture()

        assertTrue(updatedState.isCapturingPhoto)
        assertNull(updatedState.photoCaptureError)
        assertEquals(samplePhoto, updatedState.evidencePhoto)
    }

    @Test
    fun `storing a captured photo stops the capture and clears the error`() {
        val newPhoto = samplePhoto.copy(fileName = "evidence_2.jpg")
        val state = CreateReportFormState(
            evidencePhoto = samplePhoto,
            isCapturingPhoto = true,
            photoCaptureError = "Error previo"
        )

        val updatedState = state.updateEvidencePhoto(newPhoto)

        assertEquals(newPhoto, updatedState.evidencePhoto)
        assertFalse(updatedState.isCapturingPhoto)
        assertNull(updatedState.photoCaptureError)
    }

    @Test
    fun `cancelling a capture keeps the previous photo without an error`() {
        val state = CreateReportFormState(evidencePhoto = samplePhoto).startPhotoCapture()

        val updatedState = state.cancelPhotoCapture()

        assertFalse(updatedState.isCapturingPhoto)
        assertEquals(samplePhoto, updatedState.evidencePhoto)
        assertNull(updatedState.photoCaptureError)
    }

    @Test
    fun `failing a capture exposes the message and keeps the previous photo`() {
        val state = CreateReportFormState(
            evidencePhoto = samplePhoto,
            isCapturingPhoto = true
        )

        val updatedState = state.failPhotoCapture("Se necesita permiso de cámara")

        assertFalse(updatedState.isCapturingPhoto)
        assertEquals("Se necesita permiso de cámara", updatedState.photoCaptureError)
        assertEquals(samplePhoto, updatedState.evidencePhoto)
    }
}

private val sampleCategory = Category(
    id = "category-id",
    name = "Alumbrado público",
    description = "",
    basePriority = 0,
    isCriticalEmergency = false,
    officialContactChannel = null
)

private val samplePhoto = IncidentPhotoEvidence(
    localUri = "content://cl.uct.ojociudadano.fileprovider/evidences/evidence_1.jpg",
    fileName = "evidence_1.jpg",
    sizeBytes = 1024L,
    capturedAt = 1_700_000_000_000L
)
