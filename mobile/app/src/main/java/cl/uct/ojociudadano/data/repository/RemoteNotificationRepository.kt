package cl.uct.ojociudadano.data.repository

import cl.uct.ojociudadano.data.mapper.toDomain
import cl.uct.ojociudadano.data.model.Notification
import cl.uct.ojociudadano.data.remote.ApiService
import cl.uct.ojociudadano.data.remote.error.executeApiCall
import cl.uct.ojociudadano.data.session.SessionIdentity
import cl.uct.ojociudadano.data.session.SessionManager
import cl.uct.ojociudadano.data.session.SessionRequiredException
import cl.uct.ojociudadano.data.session.SessionState
import cl.uct.ojociudadano.data.session.requireIdentity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

class RemoteNotificationRepository(
    private val apiService: ApiService,
    private val sessionManager: SessionManager,
    scope: CoroutineScope
) : NotificationRepository {

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    private val _unreadCount = MutableStateFlow(0)

    override val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()
    override val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    /**
     * Serializa las consultas: el ciclo periódico y la pantalla pueden pedir un
     * refresco al mismo tiempo y solo debe existir una petición en vuelo.
     */
    private val refreshMutex = Mutex()

    private var currentUserId: String? = null
    private var observedSession = false

    init {
        // Al cerrar sesión o cambiar de usuario no debe quedar nada del anterior.
        scope.launch {
            sessionManager.state.collect { state ->
                val userId = (state as? SessionState.Authenticated)?.session?.userId
                // Sin sesión no debe quedar nada; con otro usuario tampoco.
                if (userId == null || (observedSession && userId != currentUserId)) clear()
                currentUserId = userId
                observedSession = true
            }
        }
    }

    override suspend fun refresh(): List<Notification> {
        val rows = refreshMutex.withLock {
            val response = withCurrentIdentity { identity ->
                apiService.getNotifications(expectedSession = identity.token)
            }
            response.map { it.toDomain() }
        }
        publish(rows)
        return rows
    }

    override suspend fun markAsRead(id: String) {
        withCurrentIdentity { identity ->
            val response = apiService.markNotificationAsRead(id, expectedSession = identity.token)
            // executeApiCall traduce el HttpException a ApiException.Http; se
            // lanza para no publicar como leída una notificación que el backend
            // rechazó (p. ej. 404 mientras el endpoint no está desplegado).
            if (!response.isSuccessful) throw HttpException(response)
        }
        publish(_notifications.value.map { row ->
            if (row.id == id) row.copy(isRead = true) else row
        })
    }

    override suspend fun markAllAsRead(): Int {
        var marked = 0
        // El contrato masivo aún no está definido en el backend; se reutiliza la
        // operación individual y un fallo puntual no cancela el resto.
        _notifications.value.filter { !it.isRead }.forEach { row ->
            try {
                markAsRead(row.id)
                marked++
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                // Silencioso: la notificación permanece pendiente.
            }
        }
        return marked
    }

    override fun clear() {
        publish(emptyList())
    }

    private fun publish(rows: List<Notification>) {
        _notifications.value = rows
        _unreadCount.value = rows.count { !it.isRead }
    }

    private suspend fun <T> withCurrentIdentity(block: suspend (SessionIdentity) -> T): T {
        val identity = sessionManager.requireIdentity()
        val result = executeApiCall { block(identity) }
        // A late response must not be published into another user's session.
        if (runCatching { sessionManager.requireIdentity() }.getOrNull() != identity) {
            throw SessionRequiredException()
        }
        return result
    }
}
