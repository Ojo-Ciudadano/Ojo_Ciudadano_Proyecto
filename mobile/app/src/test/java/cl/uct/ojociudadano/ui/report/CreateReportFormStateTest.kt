package cl.uct.ojociudadano.ui.report

import cl.uct.ojociudadano.data.model.Category
import cl.uct.ojociudadano.data.model.IncidentPhotoEvidence
import cl.uct.ojociudadano.domain.model.GeoCoordinate
import cl.uct.ojociudadano.domain.model.LocationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateReportFormStateTest {

    @Test
    fun `updating title clears only its previous error and keeps the photos`() {
        val state = CreateReportFormState(
            title = "",
            description = "",
            evidencePhotos = listOf(samplePhoto),
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
        assertEquals(listOf(samplePhoto), updatedState.evidencePhotos)
    }

    @Test
    fun `updating description clears only its previous error and keeps the photos`() {
        val state = CreateReportFormState(
            title = "Poste dañado",
            description = "",
            evidencePhotos = listOf(samplePhoto),
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
        assertEquals(listOf(samplePhoto), updatedState.evidencePhotos)
    }

    @Test
    fun `updating category stores selection and clears only its previous error`() {
        val category = sampleCategory
        val state = CreateReportFormState(
            title = "Poste dañado",
            description = "El poste bloquea la vereda",
            evidencePhotos = listOf(samplePhoto),
            titleError = "El título es obligatorio",
            descriptionError = "La descripción es obligatoria",
            categoryError = "La categoría es obligatoria"
        )

        val updatedState = state.updateCategory(category)

        assertEquals(category, updatedState.selectedCategory)
        assertNull(updatedState.categoryError)
        assertEquals(state.titleError, updatedState.titleError)
        assertEquals(state.descriptionError, updatedState.descriptionError)
        assertEquals(listOf(samplePhoto), updatedState.evidencePhotos)
    }

    @Test
    fun `starts without evidence or photo errors`() {
        val state = CreateReportFormState()

        assertTrue(state.evidencePhotos.isEmpty())
        assertNull(state.evidencePhoto)
        assertNull(state.activePhotoOperation)
        assertNull(state.photoError)
    }

    @Test
    fun `starting a photo operation clears the previous error and keeps the collection`() {
        val state = CreateReportFormState(
            evidencePhotos = listOf(samplePhoto),
            activePhotoOperation = null,
            photoError = "No fue posible abrir la cámara"
        )

        val updatedState = state.startPhotoOperation(PhotoOperation.CAMERA)

        assertEquals(PhotoOperation.CAMERA, updatedState.activePhotoOperation)
        assertNull(updatedState.photoError)
        assertEquals(listOf(samplePhoto), updatedState.evidencePhotos)
    }

    @Test
    fun `adding a photo stops the operation and clears the error`() {
        val state = CreateReportFormState(
            evidencePhotos = listOf(samplePhoto),
            activePhotoOperation = PhotoOperation.CAMERA,
            photoError = "Error previo"
        )

        val updatedState = state.addEvidencePhoto(secondSamplePhoto)

        assertEquals(
            listOf(samplePhoto, secondSamplePhoto),
            updatedState.evidencePhotos
        )
        assertNull(updatedState.activePhotoOperation)
        assertNull(updatedState.photoError)
    }

    @Test
    fun `adding a photo preserves the rest of the form`() {
        val state = CreateReportFormState(
            title = "Poste dañado",
            description = "El poste bloquea la vereda",
            selectedCategory = sampleCategory
        )

        val updatedState = state.addEvidencePhoto(samplePhoto)

        assertEquals("Poste dañado", updatedState.title)
        assertEquals("El poste bloquea la vereda", updatedState.description)
        assertEquals(sampleCategory, updatedState.selectedCategory)
        assertEquals(listOf(samplePhoto), updatedState.evidencePhotos)
    }

    @Test
    fun `adding a duplicate file name replaces the existing photo without duplicating`() {
        val state = CreateReportFormState(evidencePhotos = listOf(samplePhoto))
        val replacement = samplePhoto.copy(sizeBytes = 4096L)

        val updatedState = state.addEvidencePhoto(replacement)

        assertEquals(listOf(replacement), updatedState.evidencePhotos)
    }

    @Test
    fun `does not add more than the maximum number of photos`() {
        val photos = (1..MAX_EVIDENCE_PHOTOS).map { index ->
            samplePhoto.copy(fileName = "evidence_$index.jpg")
        }
        val state = CreateReportFormState(evidencePhotos = photos)
        val extraPhoto = samplePhoto.copy(fileName = "evidence_extra.jpg")

        val updatedState = state.addEvidencePhoto(extraPhoto)

        assertEquals(MAX_EVIDENCE_PHOTOS, updatedState.evidencePhotos.size)
        assertEquals(photos, updatedState.evidencePhotos)
        assertEquals(MAX_EVIDENCE_PHOTOS_ERROR, updatedState.photoError)
        assertNull(updatedState.activePhotoOperation)
    }

    @Test
    fun `allows replacing an existing photo when the maximum is reached`() {
        val photos = (1..MAX_EVIDENCE_PHOTOS).map { index ->
            samplePhoto.copy(fileName = "evidence_$index.jpg")
        }
        val state = CreateReportFormState(evidencePhotos = photos)
        val replacement = photos.first().copy(sizeBytes = 999L)

        val updatedState = state.addEvidencePhoto(replacement)

        assertEquals(MAX_EVIDENCE_PHOTOS, updatedState.evidencePhotos.size)
        assertEquals(
            replacement,
            updatedState.evidencePhotos.first { it.fileName == replacement.fileName }
        )
        assertNull(updatedState.photoError)
    }

    @Test
    fun `removing a photo keeps the rest of the collection`() {
        val state = CreateReportFormState(
            evidencePhotos = listOf(samplePhoto, secondSamplePhoto, thirdSamplePhoto)
        )

        val updatedState = state.removeEvidencePhoto(secondSamplePhoto.fileName)

        assertEquals(
            listOf(samplePhoto, thirdSamplePhoto),
            updatedState.evidencePhotos
        )
        assertNull(updatedState.photoError)
    }

    @Test
    fun `removing an unknown photo does not change the state`() {
        val state = CreateReportFormState(
            evidencePhotos = listOf(samplePhoto),
            photoError = "Error previo"
        )

        val updatedState = state.removeEvidencePhoto("missing.jpg")

        assertEquals(state, updatedState)
    }

    @Test
    fun `removing the last photo leaves an empty collection`() {
        val updatedState = CreateReportFormState(evidencePhotos = listOf(samplePhoto))
            .removeEvidencePhoto(samplePhoto.fileName)

        assertTrue(updatedState.evidencePhotos.isEmpty())
        assertNull(updatedState.evidencePhoto)
    }

    @Test
    fun `reporting a preview error keeps the collection`() {
        val state = CreateReportFormState(evidencePhotos = listOf(samplePhoto))
        val message = "No fue posible mostrar la fotografía"

        val updatedState = state.reportPhotoPreviewError(message)

        assertEquals(listOf(samplePhoto), updatedState.evidencePhotos)
        assertEquals(message, updatedState.photoError)
    }

    @Test
    fun `cancelling a photo operation keeps the collection without an error`() {
        val state = CreateReportFormState(evidencePhotos = listOf(samplePhoto))
            .startPhotoOperation(PhotoOperation.CAMERA)

        val updatedState = state.cancelPhotoOperation()

        assertNull(updatedState.activePhotoOperation)
        assertEquals(listOf(samplePhoto), updatedState.evidencePhotos)
        assertNull(updatedState.photoError)
    }

    @Test
    fun `failing a photo operation exposes the message and keeps the collection`() {
        val state = CreateReportFormState(
            evidencePhotos = listOf(samplePhoto),
            activePhotoOperation = PhotoOperation.CAMERA
        )

        val updatedState = state.failPhotoOperation("Se necesita permiso de cámara")

        assertNull(updatedState.activePhotoOperation)
        assertEquals("Se necesita permiso de cámara", updatedState.photoError)
        assertEquals(listOf(samplePhoto), updatedState.evidencePhotos)
    }

    @Test
    fun `manual location stores coordinates and clears location errors`() {
        val state = CreateReportFormState(
            location = LocationResult.Error("GPS no disponible"),
            isLoadingLocation = true,
            locationError = "GPS no disponible"
        )

        val updatedState = state.updateManualLocation(
            GeoCoordinate(latitude = -38.7396, longitude = -72.5984)
        )

        assertEquals(
            LocationResult.Success(latitude = -38.7396, longitude = -72.5984),
            updatedState.location
        )
        assertEquals(LocationSelectionSource.MANUAL, updatedState.locationSource)
        assertFalse(updatedState.isLoadingLocation)
        assertNull(updatedState.locationError)
        assertTrue(updatedState.hasValidLocation)
    }

    @Test
    fun `invalid manual location keeps the previous location and reports the problem`() {
        val previous = LocationResult.Success(latitude = -38.7359, longitude = -72.5904)
        val state = CreateReportFormState(
            location = previous,
            locationSource = LocationSelectionSource.DEVICE
        )

        val updatedState = state.updateManualLocation(
            GeoCoordinate(latitude = 91.0, longitude = -72.5904)
        )

        assertEquals(previous, updatedState.location)
        assertEquals(LocationSelectionSource.DEVICE, updatedState.locationSource)
        assertEquals(
            CreateReportLocationErrors.INVALID_COORDINATES,
            updatedState.locationError
        )
    }

    @Test
    fun `device location is identified as such and clears a previous manual source`() {
        val state = CreateReportFormState(
            locationSource = LocationSelectionSource.MANUAL,
            location = LocationResult.Success(-38.7359, -72.5904)
        )

        val updatedState = state.updateLocation(
            LocationResult.Success(latitude = -33.4489, longitude = -70.6693)
        )

        assertEquals(LocationSelectionSource.DEVICE, updatedState.locationSource)
        assertNull(updatedState.locationError)
    }

    @Test
    fun `a failed gps request keeps the manual coordinate and reports the problem`() {
        val manualLocation = LocationResult.Success(-38.7359, -72.5904)
        val state = CreateReportFormState(
            location = manualLocation,
            locationSource = LocationSelectionSource.MANUAL,
            isLoadingLocation = true
        )

        val updatedState = state.updateLocation(LocationResult.NotAvailable)

        assertEquals(manualLocation, updatedState.location)
        assertEquals(LocationSelectionSource.MANUAL, updatedState.locationSource)
        assertFalse(updatedState.isLoadingLocation)
        assertTrue(updatedState.hasValidLocation)
        assertEquals(
            CreateReportLocationErrors.NOT_AVAILABLE,
            updatedState.locationError
        )
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

private val secondSamplePhoto = samplePhoto.copy(
    localUri = "content://cl.uct.ojociudadano.fileprovider/evidences/evidence_2.jpg",
    fileName = "evidence_2.jpg",
    sizeBytes = 2048L,
    capturedAt = 1_700_000_001_000L
)

private val thirdSamplePhoto = samplePhoto.copy(
    localUri = "content://cl.uct.ojociudadano.fileprovider/evidences/evidence_3.jpg",
    fileName = "evidence_3.jpg",
    sizeBytes = 3072L,
    capturedAt = 1_700_000_002_000L
)
