package cl.uct.ojociudadano.data.repository

import cl.uct.ojociudadano.data.mapper.toDomain
import cl.uct.ojociudadano.data.mapper.toRequestDto
import cl.uct.ojociudadano.data.model.CreateIncidentInput
import cl.uct.ojociudadano.data.model.Incident
import cl.uct.ojociudadano.data.remote.ApiService
import cl.uct.ojociudadano.data.remote.error.executeApiCall

class RemoteIncidentRepository(
    private val apiService: ApiService
) : IncidentRepository {

    override suspend fun getIncidents(): List<Incident> {
        val response = executeApiCall {
            apiService.getIncidents()
        }

        return response.map { it.toDomain() }
    }

    override suspend fun getIncidentsByReporter(
        reporterUserId: String
    ): List<Incident> {
        val response = executeApiCall {
            apiService.getIncidents(reporterUserId)
        }

        return response.map { it.toDomain() }
    }

    override suspend fun getIncidentById(id: String): Incident {
        val response = executeApiCall {
            apiService.getIncidentById(id)
        }

        return response.toDomain()
    }

    override suspend fun createIncident(
        input: CreateIncidentInput
    ): Incident {
        val response = executeApiCall {
            apiService.createIncident(input.toRequestDto())
        }

        return response.toDomain()
    }
}
