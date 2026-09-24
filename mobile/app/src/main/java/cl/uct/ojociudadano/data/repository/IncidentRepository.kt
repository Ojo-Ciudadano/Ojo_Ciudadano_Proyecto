package cl.uct.ojociudadano.data.repository

import cl.uct.ojociudadano.data.model.CreateIncidentInput
import cl.uct.ojociudadano.data.model.Incident

interface IncidentRepository {

    suspend fun getIncidents(
        reporterUserId: String? = null
    ): List<Incident>

    suspend fun getIncidentById(id: String): Incident

    suspend fun createIncident(input: CreateIncidentInput): Incident
}
