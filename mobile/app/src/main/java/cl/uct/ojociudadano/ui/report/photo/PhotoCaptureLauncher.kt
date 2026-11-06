package cl.uct.ojociudadano.ui.report.photo

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import cl.uct.ojociudadano.data.media.EvidencePhotoFileStore
import cl.uct.ojociudadano.data.media.PendingPhotoCapture
import cl.uct.ojociudadano.data.model.IncidentPhotoEvidence
import cl.uct.ojociudadano.ui.report.PhotoOperationToken

/**
 * Acción con la que el formulario inicia la captura de una fotografía.
 */
fun interface PhotoCaptureLauncher {
    fun launch()
}

/**
 * Prepara el mecanismo de captura con la aplicación de cámara del dispositivo.
 *
 * Antes de abrir la cámara se verifica que exista una aplicación para capturar
 * imágenes y que el permiso [Manifest.permission.CAMERA] esté concedido. Si no
 * lo está, se solicita al usuario y solo se abre la cámara cuando lo acepta.
 *
 * El archivo temporal se reserva después de obtener el permiso y se resuelve
 * cuando la actividad de cámara devuelve el control a la aplicación. El estado
 * de la captura pendiente se conserva ante cambios de configuración para
 * recuperar correctamente la fotografía si el dispositivo rota mientras la
 * cámara está abierta.
 *
 * La cancelación y los errores se informan mediante [onCancelled] y [onError]
 * para que el formulario mantenga su propio estado sin conocer detalles del
 * permiso ni del resultado de la actividad.
 *
 * [onStart] entrega el token de la nueva operación. Ese token viaja con todos
 * los callbacks para que el formulario pueda descartar el resultado de una
 * captura que ya fue reemplazada por otra operación.
 */
@Composable
fun rememberPhotoCaptureLauncher(
    fileStore: EvidencePhotoFileStore,
    onStart: () -> PhotoOperationToken,
    onCaptured: (PhotoOperationToken, IncidentPhotoEvidence) -> Unit,
    onCancelled: (PhotoOperationToken) -> Unit,
    onError: (PhotoOperationToken, String) -> Unit
): PhotoCaptureLauncher {
    val context = LocalContext.current
    val currentOnStart by rememberUpdatedState(onStart)
    val currentOnCaptured by rememberUpdatedState(onCaptured)
    val currentOnCancelled by rememberUpdatedState(onCancelled)
    val currentOnError by rememberUpdatedState(onError)

    var pendingCapture by rememberSaveable(stateSaver = PendingPhotoCaptureSaver) {
        mutableStateOf<PendingPhotoCapture?>(null)
    }

    var activeToken by rememberSaveable {
        mutableStateOf<PhotoOperationToken?>(null)
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { captured ->
        val token = activeToken
        val pending = pendingCapture
        pendingCapture = null
        activeToken = null

        if (token == null) {
            pending?.let { stale -> fileStore.delete(stale.fileName) }
            return@rememberLauncherForActivityResult
        }

        when (val outcome = fileStore.resolveCaptureOutcome(pending, captured)) {
            is PhotoCaptureOutcome.Captured -> currentOnCaptured(token, outcome.evidence)
            PhotoCaptureOutcome.Cancelled -> currentOnCancelled(token)
            is PhotoCaptureOutcome.Failed -> currentOnError(token, outcome.message)
        }
    }

    val startCamera: (PhotoOperationToken) -> Unit = startCamera@{ token ->
        pendingCapture?.let { stale -> fileStore.delete(stale.fileName) }
        pendingCapture = null

        val capture = try {
            fileStore.create()
        } catch (exception: Exception) {
            activeToken = null
            currentOnError(
                token,
                exception.message ?: PhotoCaptureErrors.PREPARE_FILE_FAILED
            )
            return@startCamera
        }

        pendingCapture = capture

        try {
            takePictureLauncher.launch(capture.localUri.toUri())
        } catch (exception: Exception) {
            fileStore.delete(capture.fileName)
            pendingCapture = null
            activeToken = null
            currentOnError(
                token,
                if (exception is ActivityNotFoundException) {
                    PhotoCaptureErrors.CAMERA_UNAVAILABLE
                } else {
                    PhotoCaptureErrors.OPEN_CAMERA_FAILED
                }
            )
        }
    }

    val currentStartCamera by rememberUpdatedState(startCamera)

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val token = activeToken

        if (token == null) {
            return@rememberLauncherForActivityResult
        }

        if (granted) {
            currentStartCamera(token)
        } else {
            activeToken = null
            currentOnError(
                token,
                CameraPermissionPolicy.denialMessage(
                    shouldShowRationale = context.shouldShowCameraRationale()
                )
            )
        }
    }

    return remember(fileStore, takePictureLauncher, cameraPermissionLauncher, context) {
        PhotoCaptureLauncher {
            val token = currentOnStart()
            activeToken = token

            if (!context.hasCameraApplication()) {
                activeToken = null
                currentOnError(token, PhotoCaptureErrors.CAMERA_UNAVAILABLE)
                return@PhotoCaptureLauncher
            }

            if (CameraPermissionPolicy.needsRequest(context.isCameraPermissionGranted())) {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            } else {
                currentStartCamera(token)
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

private fun Context.isCameraPermissionGranted(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED

/**
 * El sistema solo puede volver a mostrar la solicitud mientras el usuario no
 * haya marcado la denegación permanente.
 */
private fun Context.shouldShowCameraRationale(): Boolean =
    findActivity()?.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) == true

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Suppress("DEPRECATION")
private fun Context.hasCameraApplication(): Boolean =
    packageManager.resolveActivity(
        Intent(MediaStore.ACTION_IMAGE_CAPTURE),
        PackageManager.MATCH_DEFAULT_ONLY
    ) != null
