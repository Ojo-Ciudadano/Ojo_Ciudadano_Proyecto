package cl.uct.ojociudadano.ui.permission

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

@Composable
fun RequestLocationPermissionEffect(
    viewModel: LocationPermissionViewModel,
    onPermissionGranted: () -> Unit
) {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        val isGranted = fineGranted || coarseGranted

        if (isGranted) {
            viewModel.updatePermissionState(isGranted = true)
            onPermissionGranted()
        } else {
            // Aquí puedes evaluar si fue denegado permanentemente
            // (verificando si el usuario marcó "No volver a preguntar" o si el sistema bloqueó el diálogo)
            viewModel.updatePermissionState(isGranted = false)
        }
    }

    // Función para disparar la solicitud cuando el flujo lo requiera
    fun launchPermissionRequest() {
        launcher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }
}