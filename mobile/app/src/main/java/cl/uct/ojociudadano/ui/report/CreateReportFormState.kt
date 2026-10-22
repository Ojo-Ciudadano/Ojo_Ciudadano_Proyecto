package cl.uct.ojociudadano.ui.report

import cl.uct.ojociudadano.data.model.Category
import cl.uct.ojociudadano.data.model.IncidentPhotoEvidence

data class CreateReportFormState(
    val title: String = "",
    val description: String = "",
    val selectedCategory: Category? = null,
    val evidencePhoto: IncidentPhotoEvidence? = null,
    val isCapturingPhoto: Boolean = false,
    val titleError: String? = null,
    val descriptionError: String? = null,
    val categoryError: String? = null,
    val photoCaptureError: String? = null
) {

    fun updateTitle(title: String): CreateReportFormState =
        copy(title = title, titleError = null)

    fun updateDescription(description: String): CreateReportFormState =
        copy(description = description, descriptionError = null)

    fun updateCategory(category: Category): CreateReportFormState =
        copy(selectedCategory = category, categoryError = null)

    fun startPhotoCapture(): CreateReportFormState =
        copy(isCapturingPhoto = true, photoCaptureError = null)

    fun updateEvidencePhoto(photo: IncidentPhotoEvidence): CreateReportFormState =
        copy(evidencePhoto = photo, isCapturingPhoto = false, photoCaptureError = null)

    fun cancelPhotoCapture(): CreateReportFormState =
        copy(isCapturingPhoto = false)

    fun failPhotoCapture(message: String): CreateReportFormState =
        copy(isCapturingPhoto = false, photoCaptureError = message)
}
