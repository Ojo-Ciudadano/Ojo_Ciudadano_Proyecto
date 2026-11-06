package cl.uct.ojociudadano.ui.permission

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Acciones del sistema relacionadas con la ubicación que la pantalla puede
 * solicitar sin conocer los contratos de actividad de Android.
 */
class LocationPermissionLauncher internal constructor(
    private val hasPermission: () -> Boolean,
    private val requestPermission: () -> Unit,
    private val openSettings: () -> Unit
) {

    fun isGranted(): Boolean = hasPermission()

    /**
     * Muestra la solicitud de permisos de ubicación del sistema.
     */
    fun request() {
        requestPermission()
    }

    /**
     * Abre los ajustes de ubicación del dispositivo para que el usuario active
     * el GPS. Android no permite activarlo directamente desde la aplicación.
     */
    fun openLocationSettings() {
        openSettings()
    }
}

/**
 * Prepara la solicitud de permisos de ubicación y la apertura de sus ajustes.
 *
 * [onPermissionResult] recibe `true` cuando el usuario concede ubicación
 * precisa o aproximada. [onReturnFromSettings] se invoca al volver de la
 * pantalla de ajustes para que el formulario pueda volver a consultar el GPS.
 */
@Composable
fun rememberLocationPermissionLauncher(
    onPermissionResult: (Boolean) -> Unit,
    onReturnFromSettings: () -> Unit = {}
): LocationPermissionLauncher {
    val context = LocalContext.current
    val currentOnPermissionResult by rememberUpdatedState(onPermissionResult)
    val currentOnReturnFromSettings by rememberUpdatedState(onReturnFromSettings)

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        currentOnPermissionResult(
            LocationPermissionPolicy.isGranted(
                fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true,
                coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            )
        )
    }

    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        currentOnReturnFromSettings()
    }

    return remember(context, permissionLauncher, settingsLauncher) {
        LocationPermissionLauncher(
            hasPermission = { context.hasLocationPermission() },
            requestPermission = {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            },
            openSettings = {
                settingsLauncher.launch(
                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                )
            }
        )
    }
}

private fun Context.hasLocationPermission(): Boolean =
    LocationPermissionPolicy.isGranted(
        fineGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED,
        coarseGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    )
