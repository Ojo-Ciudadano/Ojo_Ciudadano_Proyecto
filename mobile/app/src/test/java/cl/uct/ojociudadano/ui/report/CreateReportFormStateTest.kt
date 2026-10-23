package cl.uct.ojociudadano.ui.report

import cl.uct.ojociudadano.data.model.Category
import cl.uct.ojociudadano.data.model.IncidentPhotoEvidence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun `starts without evidence or photo errors`() {
        val state = CreateReportFormState()

        assertNull(state.evidencePhoto)
        assertNull(state.activePhotoOperation)
        assertNull(state.photoError)
    }

    @Test
    fun `starting a photo operation clears the previous error and keeps the current photo`() {
        val state = CreateReportFormState(
            evidencePhoto = samplePhoto,
            activePhotoOperation = null,
            photoError = "No fue posible abrir la cámara"
        )

        val updatedState = state.startPhotoOperation(PhotoOperation.CAMERA)

        assertEquals(PhotoOperation.CAMERA, updatedState.activePhotoOperation)
        assertNull(updatedState.photoError)
        assertEquals(samplePhoto, updatedState.evidencePhoto)
    }

    @Test
    fun `storing a photo stops the operation and clears the error`() {
        val newPhoto = samplePhoto.copy(fileName = "evidence_2.jpg")
        val state = CreateReportFormState(
            evidencePhoto = samplePhoto,
            activePhotoOperation = PhotoOperation.CAMERA,
            photoError = "Error previo"
        )

        val updatedState = state.updateEvidencePhoto(newPhoto)

        assertEquals(newPhoto, updatedState.evidencePhoto)
        assertNull(updatedState.activePhotoOperation)
        assertNull(updatedState.photoError)
    }

    @Test
    fun `cancelling a photo operation keeps the previous photo without an error`() {
        val state = CreateReportFormState(evidencePhoto = samplePhoto)
            .startPhotoOperation(PhotoOperation.CAMERA)

        val updatedState = state.cancelPhotoOperation()

        assertNull(updatedState.activePhotoOperation)
        assertEquals(samplePhoto, updatedState.evidencePhoto)
        assertNull(updatedState.photoError)
    }

    @Test
    fun `failing a photo operation exposes the message and keeps the previous photo`() {
        val state = CreateReportFormState(
            evidencePhoto = samplePhoto,
            activePhotoOperation = PhotoOperation.CAMERA
        )

        val updatedState = state.failPhotoOperation("Se necesita permiso de cámara")

        assertNull(updatedState.activePhotoOperation)
        assertEquals("Se necesita permiso de cámara", updatedState.photoError)
        assertEquals(samplePhoto, updatedState.evidencePhoto)
    }

    @Test
    fun `starting a gallery operation clears the previous error and keeps the photo`() {
        val state = CreateReportFormState(
            evidencePhoto = samplePhoto,
            activePhotoOperation = PhotoOperation.CAMERA,
            photoError = "Error previo"
        )

        val updatedState = state.startPhotoOperation(PhotoOperation.GALLERY)

        assertEquals(PhotoOperation.GALLERY, updatedState.activePhotoOperation)
        assertNull(updatedState.photoError)
        assertEquals(samplePhoto, updatedState.evidencePhoto)
    }

    @Test
    fun `storing a gallery photo stops the gallery operation and clears the error`() {
        val newPhoto = samplePhoto.copy(fileName = "evidence_gallery.jpg")
        val state = CreateReportFormState(
            evidencePhoto = samplePhoto,
            activePhotoOperation = PhotoOperation.GALLERY,
            photoError = "Error previo"
        )

        val updatedState = state.updateEvidencePhoto(newPhoto)

        assertEquals(newPhoto, updatedState.evidencePhoto)
        assertNull(updatedState.activePhotoOperation)
        assertNull(updatedState.photoError)
    }

    @Test
    fun `cancelling a gallery operation keeps the previous photo without an error`() {
        val state = CreateReportFormState(evidencePhoto = samplePhoto)
            .startPhotoOperation(PhotoOperation.GALLERY)

        val updatedState = state.cancelPhotoOperation()

        assertNull(updatedState.activePhotoOperation)
        assertEquals(samplePhoto, updatedState.evidencePhoto)
        assertNull(updatedState.photoError)
    }

    @Test
    fun `failing a gallery operation exposes the message and keeps the previous photo`() {
        val state = CreateReportFormState(
            evidencePhoto = samplePhoto,
            activePhotoOperation = PhotoOperation.GALLERY
        )

        val updatedState = state.failPhotoOperation(
            "No se pudo recuperar la fotografía seleccionada"
        )

        assertNull(updatedState.activePhotoOperation)
        assertEquals(
            "No se pudo recuperar la fotografía seleccionada",
            updatedState.photoError
        )
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
