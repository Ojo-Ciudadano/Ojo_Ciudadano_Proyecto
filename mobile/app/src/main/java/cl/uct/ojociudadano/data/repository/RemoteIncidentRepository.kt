package cl.uct.ojociudadano.data.repository

import cl.uct.ojociudadano.data.mapper.toDomain
import cl.uct.ojociudadano.data.mapper.toRequestDto
import cl.uct.ojociudadano.data.model.CreateIncidentInput
import cl.uct.ojociudadano.data.model.Incident
import cl.uct.ojociudadano.data.remote.ApiService
import cl.uct.ojociudadano.data.remote.error.executeApiCall
import cl.uct.ojociudadano.data.session.SessionIdentity
import cl.uct.ojociudadano.data.session.SessionManager
import cl.uct.ojociudadano.data.session.SessionRequiredException
import cl.uct.ojociudadano.data.session.requireIdentity

class RemoteIncidentRepository(
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) : IncidentRepository {

    override suspend fun getIncidents(): List<Incident> {
        val response = withCurrentIdentity { identity ->
            apiService.getIncidents(expectedSession = identity.token)
        }

        return response.map { it.toDomain() }
    }

    override suspend fun getCurrentUserIncidents(): List<Incident> {
        val response = withCurrentIdentity { identity ->
            apiService.getIncidents(identity.userId, identity.token)
        }

        return response.map { it.toDomain() }
    }

    override suspend fun getIncidentById(id: String): Incident {
        val response = withCurrentIdentity { identity ->
            apiService.getIncidentById(id, identity.token)
        }

        return response.toDomain()
    }

    override suspend fun createIncident(
        input: CreateIncidentInput
    ): Incident {
        val response = withCurrentIdentity { identity ->
            apiService.createIncident(input.toRequestDto(identity.userId), identity.token)
        }

        return response.toDomain()
    }

    private suspend fun <T> withCurrentIdentity(block: suspend (SessionIdentity) -> T): T {
        val identity = sessionManager.requireIdentity()
        val result = executeApiCall { block(identity) }
        // A late response must not be published into another user's session.
        if (sessionManager.requireIdentity() != identity) throw SessionRequiredException()
        return result
    }
}
