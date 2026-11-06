package cl.uct.ojociudadano.data.repository

import cl.uct.ojociudadano.data.mapper.toProfile
import cl.uct.ojociudadano.data.model.UserProfile
import cl.uct.ojociudadano.data.remote.ApiService
import cl.uct.ojociudadano.data.remote.error.executeApiCall
import cl.uct.ojociudadano.data.session.SessionIdentity
import cl.uct.ojociudadano.data.session.SessionManager
import cl.uct.ojociudadano.data.session.SessionRequiredException
import cl.uct.ojociudadano.data.session.requireIdentity

/**
 * Consulta `/api/v1/usuarios/me` con el token de la sesión vigente.
 *
 * El endpoint no recibe un identificador de usuario: el backend lo resuelve
 * del claim `sub` del JWT, por lo que esta capa no puede consultar el perfil
 * de otra persona.
 */
class RemoteProfileRepository(
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) : ProfileRepository {

    override suspend fun getProfile(): UserProfile = withCurrentIdentity { identity ->
        apiService.getMyProfile(expectedSession = identity.token).toProfile()
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
