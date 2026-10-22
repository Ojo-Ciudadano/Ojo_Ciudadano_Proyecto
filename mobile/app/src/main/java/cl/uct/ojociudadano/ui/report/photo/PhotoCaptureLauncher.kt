package cl.uct.ojociudadano.ui.report.photo

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import cl.uct.ojociudadano.data.media.EvidencePhotoFileStore
import cl.uct.ojociudadano.data.media.PendingPhotoCapture
import cl.uct.ojociudadano.data.model.IncidentPhotoEvidence

/**
 * Acción con la que el formulario inicia la captura de una fotografía.
 */
fun interface PhotoCaptureLauncher {
    fun launch()
}

/**
 * Prepara el mecanismo de captura con la aplicación de cámara del dispositivo.
 *
 * El archivo temporal se reserva antes de abrir la cámara y se resuelve cuando
 * la actividad de cámara devuelve el control a la aplicación. El estado de la
 * captura pendiente se conserva ante cambios de configuración para recuperar
 * correctamente la fotografía si el dispositivo rota mientras la cámara está
 * abierta.
 *
 * No se solicita permiso de cámara en esta etapa: la aplicación de cámara
 * gestiona sus propios permisos.
 */
@Composable
fun rememberPhotoCaptureLauncher(
    fileStore: EvidencePhotoFileStore,
    onCaptured: (IncidentPhotoEvidence) -> Unit,
    onCancelled: () -> Unit,
    onError: (String) -> Unit
): PhotoCaptureLauncher {
    val context = LocalContext.current
    val currentOnCaptured by rememberUpdatedState(onCaptured)
    val currentOnCancelled by rememberUpdatedState(onCancelled)
    val currentOnError by rememberUpdatedState(onError)

    var pendingCapture by rememberSaveable(stateSaver = PendingPhotoCaptureSaver) {
        mutableStateOf<PendingPhotoCapture?>(null)
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { captured ->
        val pending = pendingCapture
        pendingCapture = null

        when (val outcome = fileStore.resolveCaptureOutcome(pending, captured)) {
            is PhotoCaptureOutcome.Captured -> currentOnCaptured(outcome.evidence)
            PhotoCaptureOutcome.Cancelled -> currentOnCancelled()
            is PhotoCaptureOutcome.Failed -> currentOnError(outcome.message)
        }
    }

    return remember(fileStore, takePictureLauncher) {
        PhotoCaptureLauncher {
            pendingCapture?.let { stale -> fileStore.delete(stale.fileName) }
            pendingCapture = null

            if (!context.hasCameraApplication()) {
                currentOnError(PhotoCaptureErrors.CAMERA_UNAVAILABLE)
                return@PhotoCaptureLauncher
            }

            val capture = try {
                fileStore.create()
            } catch (exception: Exception) {
                currentOnError(
                    exception.message ?: PhotoCaptureErrors.PREPARE_FILE_FAILED
                )
                return@PhotoCaptureLauncher
            }

            pendingCapture = capture

            try {
                takePictureLauncher.launch(Uri.parse(capture.localUri))
            } catch (exception: Exception) {
                fileStore.delete(capture.fileName)
                pendingCapture = null
                currentOnError(
                    if (exception is ActivityNotFoundException) {
                        PhotoCaptureErrors.CAMERA_UNAVAILABLE
                    } else {
                        PhotoCaptureErrors.OPEN_CAMERA_FAILED
                    }
                )
            }
        }
    }
}

/**
 * Conserva la captura pendiente ante recreaciones de la actividad.
 */
private val PendingPhotoCaptureSaver = listSaver<PendingPhotoCapture?, String>(
    save = { pending ->
        pending?.let {
            listOf(it.localUri, it.fileName, it.mimeType)
        } ?: emptyList()
    },
    restore = { values ->
        if (values.size == 3) {
            PendingPhotoCapture(
                localUri = values[0],
                fileName = values[1],
                mimeType = values[2]
            )
        } else {
            null
        }
    }
)

@Suppress("DEPRECATION")
private fun Context.hasCameraApplication(): Boolean =
    packageManager.resolveActivity(
        Intent(MediaStore.ACTION_IMAGE_CAPTURE),
        PackageManager.MATCH_DEFAULT_ONLY
    ) != null
