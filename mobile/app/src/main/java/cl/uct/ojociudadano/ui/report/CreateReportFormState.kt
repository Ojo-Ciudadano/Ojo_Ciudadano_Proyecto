package cl.uct.ojociudadano.ui.report

import cl.uct.ojociudadano.data.model.Category
import cl.uct.ojociudadano.data.model.IncidentPhotoEvidence

/**
 * Origen de la fotografía que se está incorporando al formulario.
 */
enum class PhotoOperation {
    CAMERA,
    GALLERY
}

data class CreateReportFormState(
    val title: String = "",
    val description: String = "",
    val selectedCategory: Category? = null,
    val evidencePhoto: IncidentPhotoEvidence? = null,
    val activePhotoOperation: PhotoOperation? = null,
    val titleError: String? = null,
    val descriptionError: String? = null,
    val categoryError: String? = null,
    val photoError: String? = null
) {

    fun updateTitle(title: String): CreateReportFormState =
        copy(title = title, titleError = null)

    fun updateDescription(description: String): CreateReportFormState =
        copy(description = description, descriptionError = null)

    fun updateCategory(category: Category): CreateReportFormState =
        copy(selectedCategory = category, categoryError = null)

    /**
     * Marca el inicio de una operación de fotografía y limpia el error previo.
     *
     * Cámara y galería comparten este estado para que solo una operación pueda
     * estar activa a la vez y para que el formulario bloquee sus acciones
     * mientras el sistema toma el control de la pantalla.
     */
    fun startPhotoOperation(operation: PhotoOperation): CreateReportFormState =
        copy(activePhotoOperation = operation, photoError = null)

    /**
     * Conserva la fotografía recuperada y da por finalizada la operación.
     */
    fun updateEvidencePhoto(photo: IncidentPhotoEvidence): CreateReportFormState =
        copy(
            evidencePhoto = photo,
            activePhotoOperation = null,
            photoError = null
        )

    /**
     * El usuario canceló la operación: no es un error y la fotografía anterior,
     * si existía, se mantiene disponible.
     */
    fun cancelPhotoOperation(): CreateReportFormState =
        copy(activePhotoOperation = null)

    fun failPhotoOperation(message: String): CreateReportFormState =
        copy(activePhotoOperation = null, photoError = message)
}
