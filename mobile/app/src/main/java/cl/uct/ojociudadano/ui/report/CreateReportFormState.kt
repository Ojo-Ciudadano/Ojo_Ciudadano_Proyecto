package cl.uct.ojociudadano.ui.report

import cl.uct.ojociudadano.data.model.Category
import cl.uct.ojociudadano.data.model.IncidentPhotoEvidence

/**
 * Cantidad máxima de fotografías que el formulario acepta como evidencia.
 */
const val MAX_EVIDENCE_PHOTOS = 10

/**
 * Mensaje que se muestra cuando el formulario ya alcanzó el máximo de fotos.
 */
const val MAX_EVIDENCE_PHOTOS_ERROR =
    "Puedes adjuntar hasta $MAX_EVIDENCE_PHOTOS fotografías por reporte"

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
    val evidencePhotos: List<IncidentPhotoEvidence> = emptyList(),
    val activePhotoOperation: PhotoOperation? = null,
    val titleError: String? = null,
    val descriptionError: String? = null,
    val categoryError: String? = null,
    val photoError: String? = null
) {

    /**
     * Primera evidencia de la colección.
     *
     * Se conserva como compatibilidad mientras la interfaz migra a
     * [evidencePhotos]. La vista previa definitiva consume la colección completa.
     */
    val evidencePhoto: IncidentPhotoEvidence?
        get() = evidencePhotos.firstOrNull()

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
     * Agrega una evidencia a la colección sin reemplazar las anteriores.
     *
     * Si ya se alcanzó [MAX_EVIDENCE_PHOTOS], la operación finaliza y se expone
     * un error controlado. Una evidencia con el mismo
     * [IncidentPhotoEvidence.fileName] reemplaza a la existente para evitar
     * duplicados.
     */
    fun addEvidencePhoto(photo: IncidentPhotoEvidence): CreateReportFormState {
        val withoutDuplicate = evidencePhotos.filterNot { it.fileName == photo.fileName }

        if (withoutDuplicate.size >= MAX_EVIDENCE_PHOTOS) {
            return copy(
                activePhotoOperation = null,
                photoError = MAX_EVIDENCE_PHOTOS_ERROR
            )
        }

        return copy(
            evidencePhotos = withoutDuplicate + photo,
            activePhotoOperation = null,
            photoError = null
        )
    }

    /**
     * Elimina una evidencia de la colección y conserva las demás.
     *
     * Si [fileName] no pertenece a la colección, el estado no cambia.
     */
    fun removeEvidencePhoto(fileName: String): CreateReportFormState {
        if (evidencePhotos.none { it.fileName == fileName }) {
            return this
        }

        return copy(
            evidencePhotos = evidencePhotos.filterNot { it.fileName == fileName },
            photoError = null
        )
    }

    /**
     * Informa un fallo de previsualización sin alterar la colección.
     */
    fun reportPhotoPreviewError(message: String): CreateReportFormState =
        copy(photoError = message)

    /**
     * El usuario canceló la operación: no es un error y las fotografías
     * anteriores, si existían, se mantienen disponibles.
     */
    fun cancelPhotoOperation(): CreateReportFormState =
        copy(activePhotoOperation = null)

    fun failPhotoOperation(message: String): CreateReportFormState =
        copy(activePhotoOperation = null, photoError = message)
}
