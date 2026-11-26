package cl.uct.ojociudadano.data.evidence

import cl.uct.ojociudadano.data.session.SessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Invalida la caché de imágenes de Coil cuando cambia la identidad de la
 * sesión (#280).
 *
 * Las evidencias se descargan autenticadas, así que una cuenta no debe
 * reutilizar miniaturas decodificadas por otra. La invalidación es de mejor
 * esfuerzo: si una caché no puede limpiarse, se informa el fallo sin cortar el
 * flujo de sesión.
 */
class SessionImageCacheInvalidator(
    private val clearMemoryCache: suspend () -> Unit,
    private val clearDiskCache: suspend () -> Unit,
    private val reportFailure: (Throwable) -> Unit = {}
) {

    /**
     * Limpia las cachés cada vez que la sesión pasa de una identidad a otra
     * (inicio de sesión, cierre de sesión o expiración).
     */
    fun observe(scope: CoroutineScope, sessions: StateFlow<SessionState>) {
        scope.launch {
            var current = scopeOf(sessions.value)

            sessions.collect { state ->
                val next = scopeOf(state) ?: return@collect
                val previous = current
                if (previous == null) {
                    // La sesión recién se está resolviendo: no hay una
                    // identidad anterior de la que limpiar.
                    current = next
                    return@collect
                }
                if (next != previous) {
                    current = next
                    clear()
                }
            }
        }
    }

    suspend fun clear() {
        runCatching { clearMemoryCache() }.onFailure(reportFailure)
        runCatching { clearDiskCache() }.onFailure(reportFailure)
    }

    companion object {
        /** Clave de sesión usada cuando no hay una cuenta autenticada. */
        const val ANONYMOUS_SCOPE = "anonymous"

        /**
         * Identidad a la que pertenecen las imágenes cacheadas, o `null` si la
         * sesión todavía se está restaurando y no corresponde invalidar nada.
         */
        fun scopeOf(state: SessionState): String? = when (state) {
            is SessionState.Authenticated -> "user:${state.session.userId}"
            SessionState.Unauthenticated, is SessionState.Expired -> ANONYMOUS_SCOPE
            SessionState.Restoring -> null
        }
    }
}
