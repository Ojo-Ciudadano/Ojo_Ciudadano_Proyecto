package cl.uct.ojociudadano.data.notifications

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Expone si el proceso está en primer plano sin acoplar la capa de datos a una
 * Activity concreta.
 *
 * Se alimenta de `ProcessLifecycleOwner`, que agrega el ciclo de vida de todas
 * las Activities: la sincronización periódica se pausa al pasar a segundo plano
 * y se reanuda al volver, sin servicios ni alarmas del sistema.
 */
class AppForegroundState(lifecycle: Lifecycle) {

    private val _isForeground = MutableStateFlow(
        lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    )
    val isForeground: StateFlow<Boolean> = _isForeground.asStateFlow()

    private val observer = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            _isForeground.value = true
        }

        override fun onStop(owner: LifecycleOwner) {
            _isForeground.value = false
        }
    }

    init {
        lifecycle.addObserver(observer)
    }
}
