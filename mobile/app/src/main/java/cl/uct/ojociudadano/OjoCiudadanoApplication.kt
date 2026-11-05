package cl.uct.ojociudadano

import android.app.Application
import cl.uct.ojociudadano.di.AppContainer
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader

class OjoCiudadanoApplication : Application(), SingletonImageLoader.Factory {

    val container: AppContainer by lazy {
        AppContainer(this)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader = container.imageLoader
}
