package cl.uct.ojociudadano.ui.report.photo

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/**
 * Acción con la que el formulario inicia la selección de una fotografía desde
 * la galería del dispositivo.
 */
fun interface GalleryPhotoPickerLauncher {
    fun launch()
}

/**
 * Prepara el mecanismo para seleccionar una fotografía existente.
 *
 * Usa el Photo Picker del sistema a través de
 * [ActivityResultContracts.PickVisualMedia]. En las versiones de Android donde
 * el Photo Picker no está disponible, el contrato utiliza automáticamente el
 * selector de documentos del sistema. En ningún caso se requieren permisos de
 * almacenamiento.
 *
 * El resultado se entrega como una URI de contenido válida únicamente mientras
 * la aplicación conserva el permiso temporal otorgado por el selector. La
 * recuperación y copia definitiva del contenido pertenecen al formulario, que
 * recibe la URI mediante [onSelected].
 *
 * La cancelación y los errores se informan mediante [onCancelled] y [onError]
 * para que la pantalla no conozca detalles del contrato de actividad.
 */
@Composable
fun rememberGalleryPhotoPickerLauncher(
    onSelected: (String) -> Unit,
    onCancelled: () -> Unit,
    onError: (String) -> Unit
): GalleryPhotoPickerLauncher {
    val currentOnSelected by rememberUpdatedState(onSelected)
    val currentOnCancelled by rememberUpdatedState(onCancelled)
    val currentOnError by rememberUpdatedState(onError)

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { selectedUri ->
        if (selectedUri == null) {
            currentOnCancelled()
        } else {
            currentOnSelected(selectedUri.toString())
        }
    }

    return remember(pickMediaLauncher) {
        GalleryPhotoPickerLauncher {
            try {
                pickMediaLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            } catch (exception: ActivityNotFoundException) {
                currentOnError(PhotoSelectionErrors.PICKER_UNAVAILABLE)
            } catch (exception: SecurityException) {
                currentOnError(PhotoSelectionErrors.ACCESS_DENIED)
            } catch (exception: Exception) {
                currentOnError(PhotoSelectionErrors.OPEN_GALLERY_FAILED)
            }
        }
    }
}
