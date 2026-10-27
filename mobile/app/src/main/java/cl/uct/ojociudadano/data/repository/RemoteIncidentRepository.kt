package cl.uct.ojociudadano.data.repository

import cl.uct.ojociudadano.data.mapper.toDomain
import cl.uct.ojociudadano.data.mapper.toRequestDto
import cl.uct.ojociudadano.data.model.CreateIncidentInput
import cl.uct.ojociudadano.data.model.Incident
import cl.uct.ojociudadano.data.remote.ApiService
import cl.uct.ojociudadano.data.remote.error.executeApiCall
import cl.uct.ojociudadano.domain.model.Evidence
import cl.uct.ojociudadano.domain.model.toDomain
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

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

    override suspend fun uploadEvidence(incidenteId: String, file: File, mimeType: String): Evidence {
        val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
        
        val response = executeApiCall {
            apiService.uploadEvidence(incidenteId, body)
        }
        return response.toDomain()
    }

    override suspend fun getEvidencesByIncident(incidenteId: String): List<Evidence> {
        val response = executeApiCall {
            apiService.getEvidencesByIncident(incidenteId)
        }
        return response.map { it.toDomain() }
    }

    override suspend fun getEvidenceById(id: String): Evidence {
        val response = executeApiCall {
            apiService.getEvidenceById(id)
        }
        return response.toDomain()
    }
}
