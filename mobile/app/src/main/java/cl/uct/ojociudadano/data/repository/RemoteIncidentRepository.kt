package cl.uct.ojociudadano.data.repository

import cl.uct.ojociudadano.data.mapper.toDomain
import cl.uct.ojociudadano.data.mapper.toRequestDto
import cl.uct.ojociudadano.data.model.CreateIncidentInput
import cl.uct.ojociudadano.data.model.Incident
import cl.uct.ojociudadano.data.remote.ApiService

class RemoteIncidentRepository(
    private val apiService: ApiService
) : IncidentRepository {

    override suspend fun getIncidents(
        reporterUserId: String?
    ): List<Incident> {
        return apiService
            .getIncidents(reporterUserId)
            .map { it.toDomain() }
    }

    override suspend fun getIncidentById(id: String): Incident {
        return apiService
            .getIncidentById(id)
            .toDomain()
    }

    override suspend fun createIncident(
        input: CreateIncidentInput
    ): Incident {
        return apiService
            .createIncident(input.toRequestDto())
            .toDomain()
    }
}
