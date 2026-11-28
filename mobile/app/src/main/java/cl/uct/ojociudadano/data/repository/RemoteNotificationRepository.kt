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
     * Serializa refresco y marcado: un refresco con datos antiguos no puede
     * publicarse después de un marcado confirmado y revertirlo.
     */
    private val stateMutex = Mutex()

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

    override suspend fun refresh(): List<Notification> = stateMutex.withLock {
        val response = withCurrentIdentity { identity ->
            apiService.getNotifications(expectedSession = identity.token)
        }
        val rows = response.map { it.toDomain() }
        publish(rows)
        rows
    }

    override suspend fun markAsRead(id: String) {
        stateMutex.withLock { markAsReadLocked(id) }
    }

    override suspend fun markAllAsRead(): Int = stateMutex.withLock {
        val response = withCurrentIdentity { identity ->
            apiService.markAllNotificationsAsRead(expectedSession = identity.token)
        }
        // El backend confirma cuántas filas cambió; aunque `marcadas` sea 0, ya
        // no queda ninguna pendiente de su lado y la lista local debe reflejarlo.
        publish(_notifications.value.map { it.copy(isRead = true) })
        response.marcadas
    }

    /** Requiere que el llamador ya tenga [stateMutex] tomado. */
    private suspend fun markAsReadLocked(id: String) {
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
