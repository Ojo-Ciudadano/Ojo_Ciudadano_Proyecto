package cl.uct.ojociudadano.ui.report.photo

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import cl.uct.ojociudadano.ui.report.PhotoOperationToken

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
 *
 * [onStart] entrega el token de la nueva operación. Se conserva ante cambios de
 * configuración y se devuelve junto con el resultado para que el formulario
 * pueda descartar selecciones que ya fueron reemplazadas.
 */
@Composable
fun rememberGalleryPhotoPickerLauncher(
    onStart: () -> PhotoOperationToken,
    onSelected: (PhotoOperationToken, String) -> Unit,
    onCancelled: (PhotoOperationToken) -> Unit,
    onError: (PhotoOperationToken, String) -> Unit
): GalleryPhotoPickerLauncher {
    val currentOnStart by rememberUpdatedState(onStart)
    val currentOnSelected by rememberUpdatedState(onSelected)
    val currentOnCancelled by rememberUpdatedState(onCancelled)
    val currentOnError by rememberUpdatedState(onError)

    var activeToken by rememberSaveable {
        mutableStateOf<PhotoOperationToken?>(null)
    }

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { selectedUri ->
        val token = activeToken
        activeToken = null

        if (token == null) {
            return@rememberLauncherForActivityResult
        }

        if (selectedUri == null) {
            currentOnCancelled(token)
        } else {
            currentOnSelected(token, selectedUri.toString())
        }
    }

    return remember(pickMediaLauncher) {
        GalleryPhotoPickerLauncher {
            val token = currentOnStart()
            activeToken = token

            try {
                pickMediaLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            } catch (exception: ActivityNotFoundException) {
                activeToken = null
                currentOnError(token, PhotoSelectionErrors.PICKER_UNAVAILABLE)
            } catch (exception: SecurityException) {
                activeToken = null
                currentOnError(token, PhotoSelectionErrors.ACCESS_DENIED)
            } catch (exception: Exception) {
                activeToken = null
                currentOnError(token, PhotoSelectionErrors.OPEN_GALLERY_FAILED)
            }
        }
    }
}
