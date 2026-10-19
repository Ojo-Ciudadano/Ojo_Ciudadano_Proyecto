package cl.uct.ojociudadano

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import cl.uct.ojociudadano.ui.theme.OjoCiudadanoTheme
import cl.uct.ojociudadano.ui.startup.SessionStartupGate

class MainActivity : ComponentActivity() {

    private val localNetworkPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        showApp()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (requiresLocalNetworkPermission()) {
            localNetworkPermissionLauncher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
        } else {
            showApp()
        }
    }

    private fun requiresLocalNetworkPermission(): Boolean {
        return BuildConfig.DEBUG &&
            Build.VERSION.SDK_INT >= ANDROID_17_API_LEVEL &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_LOCAL_NETWORK
            ) != PackageManager.PERMISSION_GRANTED
    }

    private fun showApp() {

        setContent {

            OjoCiudadanoTheme {

                SessionStartupGate(
                    sessionBootstrapper = (
                        application as OjoCiudadanoApplication
                    ).container.sessionBootstrapper
                )
            }
        }
    }

    private companion object {
        const val ANDROID_17_API_LEVEL = 37
    }
}
