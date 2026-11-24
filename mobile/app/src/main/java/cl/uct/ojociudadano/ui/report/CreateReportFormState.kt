package cl.uct.ojociudadano.ui.report

import cl.uct.ojociudadano.data.model.Category
import cl.uct.ojociudadano.data.model.IncidentPhotoEvidence
import cl.uct.ojociudadano.domain.model.GeoCoordinate
import cl.uct.ojociudadano.domain.model.LocationResult
import cl.uct.ojociudadano.domain.model.ReverseGeocodedAddress

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
    val isAutocompletingAddress: Boolean = false,
    val addressAutofillError: String? = null,
    val lastAutocompletedAddress: ReverseGeocodedAddress? = null,
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
     * Conserva los datos de ubicación escritos por el usuario.
     *
     * Cualquier edición manual descarta el mensaje de autocompletado para que
     * la ayuda no permanezca visible sobre datos que el usuario ya corrigió.
     */
    fun updateLocationDetails(
        referenceAddress: String,
        commune: String,
        region: String,
        sector: String
    ): CreateReportFormState = copy(
        referenceAddress = referenceAddress,
        commune = commune,
        region = region,
        sector = sector,
        locationDetailsError = null,
        addressAutofillError = null
    )

    /**
     * Marca el inicio de una consulta de geocodificación inversa.
     */
    fun startAddressAutofill(): CreateReportFormState =
        copy(isAutocompletingAddress = true, addressAutofillError = null)

    /**
     * Completa los campos vacíos o que todavía conservan la sugerencia anterior.
     *
     * Un valor escrito manualmente siempre tiene prioridad sobre la sugerencia
     * del geocodificador, incluso si la respuesta llega después de la edición.
     * Si el usuario cambia de ubicación, los valores que solo habían sido
     * autocompletados se actualizan, mientras que las ediciones manuales se
     * mantienen. [sector] no se completa porque el proveedor no entrega un
     * equivalente confiable y el usuario debe precisarlo.
     */
    fun applyReverseGeocodedAddress(address: ReverseGeocodedAddress): CreateReportFormState {
        if (!address.hasAnyValue) {
            return finishAddressAutofill()
        }

        val previousAddress = lastAutocompletedAddress
        val nextAddress = ReverseGeocodedAddress(
            referenceAddress = address.referenceAddress.normalized(),
            commune = address.commune.normalized(),
            region = address.region.normalized()
        )

        return copy(
            referenceAddress = referenceAddress.updatedWith(
                previousAutocompletedValue = previousAddress?.referenceAddress,
                newValue = nextAddress.referenceAddress
            ),
            commune = commune.updatedWith(
                previousAutocompletedValue = previousAddress?.commune,
                newValue = nextAddress.commune
            ),
            region = region.updatedWith(
                previousAutocompletedValue = previousAddress?.region,
                newValue = nextAddress.region
            ),
            lastAutocompletedAddress = nextAddress,
            isAutocompletingAddress = false,
            addressAutofillError = null
        )
    }

    /**
     * Finaliza la consulta sin datos sin modificar los campos del formulario.
     */
    fun finishAddressAutofill(): CreateReportFormState =
        copy(isAutocompletingAddress = false)

    /**
     * Informa un fallo de autocompletado sin convertirlo en un error de
     * validación: el usuario puede escribir los datos manualmente y enviar.
     */
    fun failAddressAutofill(message: String): CreateReportFormState =
        copy(
            isAutocompletingAddress = false,
            addressAutofillError = message
        )

    /**
     * Marca el inicio de una consulta de ubicación y limpia el error previo.
     */
    fun startLocationRequest(): CreateReportFormState =
        copy(
            isLoadingLocation = true,
            locationError = null,
            isAutocompletingAddress = false,
            addressAutofillError = null
        )

    /**
     * Conserva el resultado del GPS en el formulario hasta que el incidente se
     * cree. Un resultado sin coordenadas deja un mensaje accionable para el
     * usuario y nunca se reemplaza por una ubicación inventada.
     */
    fun updateLocation(location: LocationResult): CreateReportFormState =
        copy(
            location = location,
            locationSource = if (location is LocationResult.Success) {
                LocationSelectionSource.DEVICE
            } else {
                LocationSelectionSource.NONE
            },
            isLoadingLocation = false,
            locationError = CreateReportLocationErrors.messageFor(location),
            isAutocompletingAddress = false
        )

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

/**
 * Conserva una edición manual y actualiza solo los valores que seguían siendo
 * una sugerencia automática o que todavía estaban vacíos.
 */
private fun String.updatedWith(
    previousAutocompletedValue: String?,
    newValue: String?
): String {
    val canReplace = isBlank() ||
        (previousAutocompletedValue != null && this == previousAutocompletedValue)

    return if (canReplace) newValue.orEmpty() else this
}

private fun String?.normalized(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
