package cl.uct.ojociudadano.ui.report

import cl.uct.ojociudadano.data.model.Category
import cl.uct.ojociudadano.data.model.IncidentPhotoEvidence
import cl.uct.ojociudadano.domain.model.GeoCoordinate
import cl.uct.ojociudadano.domain.model.LocationResult

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

/**
 * Origen de la coordenada conservada por el formulario.
 *
 * Permite distinguir la lectura del dispositivo de una selección manual y
 * evita que una respuesta tardía del GPS reemplace la elección del usuario.
 */
enum class LocationSelectionSource {
    NONE,
    DEVICE,
    MANUAL
}

data class CreateReportFormState(
    val referenceAddress: String = "",
    val commune: String = "",
    val region: String = "",
    val sector: String = "",
    val locationDetailsError: String? = null,
    val title: String = "",
    val description: String = "",
    val selectedCategory: Category? = null,
    val evidencePhotos: List<IncidentPhotoEvidence> = emptyList(),
    val activePhotoOperation: PhotoOperation? = null,
    val location: LocationResult = LocationResult.NotAvailable,
    val locationSource: LocationSelectionSource = LocationSelectionSource.NONE,
    val isLoadingLocation: Boolean = false,
    val locationError: String? = null,
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

    /**
     * Indica si el formulario conserva coordenadas listas para asociarse al
     * incidente que se enviará.
     */
    val hasValidLocation: Boolean
        get() = location is LocationResult.Success

    fun updateTitle(title: String): CreateReportFormState =
        copy(title = title, titleError = null)

    fun updateDescription(description: String): CreateReportFormState =
        copy(description = description, descriptionError = null)

    fun updateCategory(category: Category): CreateReportFormState =
        copy(selectedCategory = category, categoryError = null)

    /**
     * Marca el inicio de una consulta de ubicación y limpia el error previo.
     */
    fun startLocationRequest(): CreateReportFormState =
        copy(isLoadingLocation = true, locationError = null)

    /**
     * Conserva el resultado del GPS en el formulario hasta que el incidente se
     * cree. Un resultado sin coordenadas deja un mensaje accionable para el
     * usuario y nunca se reemplaza por una ubicación inventada.
     *
     * Si el GPS falla después de una selección manual, la coordenada elegida en
     * el mapa se conserva: solo una lectura exitosa reemplaza su origen.
     */
    fun updateLocation(location: LocationResult): CreateReportFormState {
        val keepsManualSelection = locationSource == LocationSelectionSource.MANUAL &&
            location !is LocationResult.Success

        return copy(
            location = if (keepsManualSelection) this.location else location,
            locationSource = when {
                keepsManualSelection -> LocationSelectionSource.MANUAL
                location is LocationResult.Success -> LocationSelectionSource.DEVICE
                else -> LocationSelectionSource.NONE
            },
            isLoadingLocation = false,
            locationError = CreateReportLocationErrors.messageFor(location)
        )
    }

    /**
     * Conserva una coordenada confirmada manualmente en el mapa.
     *
     * La selección manual reemplaza a la última lectura del GPS y limpia el
     * error de ubicación. Si la coordenada no cumple los rangos geográficos, la
     * ubicación anterior se mantiene y se expone un mensaje accionable.
     */
    fun updateManualLocation(coordinate: GeoCoordinate): CreateReportFormState {
        if (!coordinate.isValid) {
            return copy(
                isLoadingLocation = false,
                locationError = CreateReportLocationErrors.INVALID_COORDINATES
            )
        }

        return copy(
            location = LocationResult.Success(
                latitude = coordinate.latitude,
                longitude = coordinate.longitude
            ),
            locationSource = LocationSelectionSource.MANUAL,
            isLoadingLocation = false,
            locationError = null
        )
    }

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
     * Informa que una o más fotografías no cumplen los límites del backend.
     *
     * La colección se conserva para que el usuario pueda eliminar o reemplazar
     * las fotografías señaladas antes de volver a enviar el formulario.
     */
    fun reportEvidenceValidationError(message: String): CreateReportFormState =
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
