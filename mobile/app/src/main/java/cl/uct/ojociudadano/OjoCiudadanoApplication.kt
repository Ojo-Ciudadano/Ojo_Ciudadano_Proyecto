package cl.uct.ojociudadano

import android.app.Application
import cl.uct.ojociudadano.di.AppContainer

class OjoCiudadanoApplication : Application() {
    val container: AppContainer by lazy {
        AppContainer()
    }
}
