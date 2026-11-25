package cl.uct.ojociudadano.data.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Notificaciones globales del cliente HTTP. El buffer permite que OkHttp emita
 * los eventos sin esperar al gestor de sesión.
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

    internal fun observeIn(scope: CoroutineScope, sessionManager: SessionManager) {
        // Start collecting before the application can send its first protected request.
        scope.launch(Dispatchers.IO, start = CoroutineStart.UNDISPATCHED) {
            events.collect(sessionManager::invalidateIfCurrent)
        }
    }
}
