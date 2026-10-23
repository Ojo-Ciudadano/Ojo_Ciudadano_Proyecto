package cl.uct.ojociudadano.ui.report.photo

/**
 * Mensajes que el formulario muestra cuando la selección desde la galería no
 * puede completarse.
 *
 * Se centralizan aquí para que el selector y el formulario no dependan de
 * textos dispersos en la interfaz y para poder reutilizarlos en las pruebas.
 */
object PhotoSelectionErrors {

    const val PICKER_UNAVAILABLE =
        "No se encontró un selector de fotografías en el dispositivo"

    const val OPEN_GALLERY_FAILED = "No fue posible abrir la galería"

    const val ACCESS_DENIED = "No se pudo acceder a la fotografía seleccionada"

    const val NOT_AN_IMAGE = "El archivo seleccionado no es una imagen"

    const val EMPTY_FILE = "La fotografía seleccionada está vacía"

    const val FILE_TOO_LARGE =
        "La fotografía seleccionada supera el tamaño máximo permitido"

    const val RECOVER_PHOTO_FAILED = "No se pudo recuperar la fotografía seleccionada"
}
