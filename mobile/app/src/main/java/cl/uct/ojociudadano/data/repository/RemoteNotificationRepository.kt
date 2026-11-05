package cl.uct.ojociudadano.data.repository

import cl.uct.ojociudadano.data.mapper.toDomain
import cl.uct.ojociudadano.data.model.Notification
import cl.uct.ojociudadano.data.remote.ApiService
import cl.uct.ojociudadano.data.remote.error.executeApiCall
import cl.uct.ojociudadano.data.session.SessionIdentity
import cl.uct.ojociudadano.data.session.SessionManager
import cl.uct.ojociudadano.data.session.SessionRequiredException
import cl.uct.ojociudadano.data.session.requireIdentity

class RemoteNotificationRepository(
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) : NotificationRepository {

    override suspend fun getNotifications(): List<Notification> {
        val response = withCurrentIdentity { identity ->
            apiService.getNotifications(expectedSession = identity.token)
        }
        return response.map { it.toDomain() }
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
