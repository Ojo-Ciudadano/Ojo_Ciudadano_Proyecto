package cl.uct.ojociudadano.data.notifications

import cl.uct.ojociudadano.data.repository.NotificationRepository
import cl.uct.ojociudadano.data.session.SessionManager
import cl.uct.ojociudadano.data.session.SessionState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Ciclo de sincronización periódica de notificaciones.
 *
 * Solo consulta mientras exista una sesión autenticada **y** la aplicación esté
 * en primer plano. Al cerrar sesión o pasar a segundo plano, `collectLatest`
 * cancela el ciclo inmediatamente; no quedan corrutinas ni peticiones huérfanas.
 * Los fallos se descartan en silencio: el usuario nunca ve un diálogo por una
 * sincronización de fondo.
 */
class NotificationSyncScheduler(
    private val repository: NotificationRepository,
    private val sessionManager: SessionManager,
    private val foreground: StateFlow<Boolean>,
    private val scope: CoroutineScope,
    private val intervalMillis: Long = DEFAULT_INTERVAL_MILLIS
) {

    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            combine(
                sessionManager.state.map { it is SessionState.Authenticated },
                foreground
            ) { authenticated, inForeground -> authenticated && inForeground }
                .distinctUntilChanged()
                .collectLatest { active -> if (active) runCycle() }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private suspend fun runCycle() {
        while (true) {
            // El repositorio serializa la consulta con la recarga de la pantalla:
            // nunca hay dos peticiones simultáneas.
            try {
                repository.refresh()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                // Silencioso a propósito: el siguiente ciclo vuelve a intentarlo.
            }
            delay(intervalMillis)
        }
    }

    companion object {
        const val DEFAULT_INTERVAL_MILLIS = 60_000L
    }
}
