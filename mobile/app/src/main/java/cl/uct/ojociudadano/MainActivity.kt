package cl.uct.ojociudadano

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import cl.uct.ojociudadano.ui.startup.SessionStartupGate
import cl.uct.ojociudadano.ui.theme.OjoCiudadanoTheme

class MainActivity : ComponentActivity() {
    private val container get() = (application as OjoCiudadanoApplication).container

    private val localNetworkPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        showApp()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        updateSystemBars()

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

    private fun updateSystemBars() {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT, ::usesDarkTheme),
            navigationBarStyle = SystemBarStyle.auto(0xE6FFFFFF.toInt(), 0x801B1B1B.toInt(), ::usesDarkTheme)
        )
    }

    private fun usesDarkTheme(resources: Resources): Boolean {
        val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return container.themePreferences.mode.value.isDark(nightMode == Configuration.UI_MODE_NIGHT_YES)
    }

    private fun showApp() {
        setContent {
            val mode by container.themePreferences.mode.collectAsState()
            val darkTheme = mode.isDark(isSystemInDarkTheme())
            LaunchedEffect(darkTheme) { updateSystemBars() }

            OjoCiudadanoTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    SessionStartupGate(
                        sessionManager = container.sessionManager,
                        authRepository = container.authRepository
                    )
                }
            }
        }
    }

    private companion object {
        const val ANDROID_17_API_LEVEL = 37
    }
}
