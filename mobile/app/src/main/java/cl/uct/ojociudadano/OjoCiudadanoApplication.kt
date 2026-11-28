package cl.uct.ojociudadano

import android.app.Application
import android.app.UiModeManager
import android.os.Build
import cl.uct.ojociudadano.data.preferences.ThemeMode
import cl.uct.ojociudadano.data.session.SessionExpirationEvents
import cl.uct.ojociudadano.di.AppContainer
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OjoCiudadanoApplication : Application(), SingletonImageLoader.Factory {

    val container: AppContainer by lazy {
        AppContainer(this)
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val uiModeManager = getSystemService(UiModeManager::class.java)
            container.applicationScope.launch(Dispatchers.Main.immediate) {
                container.themePreferences.mode.collect { mode ->
                    uiModeManager.setApplicationNightMode(
                        when (mode) {
                            // En la API por aplicación, AUTO retira la configuración local.
                            ThemeMode.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
                            ThemeMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
                            ThemeMode.DARK -> UiModeManager.MODE_NIGHT_YES
                        }
                    )
                }
            }
        }
        SessionExpirationEvents.observeIn(container.applicationScope, container.sessionManager)
        // El ciclo se mantiene inactivo mientras no haya sesión o la app esté en
        // segundo plano; ver NotificationSyncScheduler.
        container.notificationSyncScheduler.start()
        // Las imágenes autenticadas no deben sobrevivir a un cambio de cuenta.
        container.imageCacheInvalidator.observe(
            container.applicationScope,
            container.sessionManager.state
        )
        container.applicationScope.launch(Dispatchers.IO) {
            container.evidenceTempCleaner.cleanupExpired()
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader = container.imageLoader
}
