package cl.uct.ojociudadano.data.session

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Notificaciones globales del cliente HTTP, sin modificar la sesión ni navegar.
 * El consumidor debe comprobar el snapshot antes de actuar sobre la sesión (#291).
 * No se reproducen eventos antiguos a nuevos observadores. El buffer evita bloquear OkHttp.
 */
object SessionExpirationEvents {
    private val mutableEvents = MutableSharedFlow<TokenSnapshot>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val events: SharedFlow<TokenSnapshot> = mutableEvents.asSharedFlow()

    fun notifyExpired(rejectedToken: TokenSnapshot) {
        mutableEvents.tryEmit(rejectedToken)
    }
}
