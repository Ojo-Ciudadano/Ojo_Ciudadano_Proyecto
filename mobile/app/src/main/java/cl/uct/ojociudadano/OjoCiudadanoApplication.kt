package cl.uct.ojociudadano

import android.app.Application
import cl.uct.ojociudadano.data.session.SessionExpirationEvents
import cl.uct.ojociudadano.di.AppContainer
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader

class OjoCiudadanoApplication : Application(), SingletonImageLoader.Factory {

    val container: AppContainer by lazy {
        AppContainer(this)
    }

    override fun onCreate() {
        super.onCreate()
        SessionExpirationEvents.observeIn(container.applicationScope, container.sessionManager)
        // El ciclo se mantiene inactivo mientras no haya sesión o la app esté en
        // segundo plano; ver NotificationSyncScheduler.
        container.notificationSyncScheduler.start()
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader = container.imageLoader
}
