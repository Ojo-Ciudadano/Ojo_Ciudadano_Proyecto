package cl.uct.ojociudadano.data.repository

import cl.uct.ojociudadano.data.mapper.toDomain
import cl.uct.ojociudadano.data.mapper.toRequestDto
import cl.uct.ojociudadano.data.model.CreateIncidentInput
import cl.uct.ojociudadano.data.model.Incident
import cl.uct.ojociudadano.data.remote.ApiService
import cl.uct.ojociudadano.data.remote.error.executeApiCall
import cl.uct.ojociudadano.domain.model.Evidence
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
import java.io.File

class RemoteIncidentRepository(
    private val apiService: ApiService
) : IncidentRepository {

    companion object {
        private const val MAX_BACKEND_FILE_SIZE = 10L * 1024L * 1024L
        private val ALLOWED_MIME_TYPES = setOf("image/jpeg", "image/png", "image/webp")
    }

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
        if (!file.exists() || !file.canRead() || file.length() <= 0L) {
            throw IllegalArgumentException("El archivo de evidencia no existe, está vacío o no es legible")
        }
        if (file.length() > MAX_BACKEND_FILE_SIZE) {
            throw IllegalArgumentException("El archivo supera el límite máximo de 10 MB aceptado por el backend")
        }
        if (mimeType !in ALLOWED_MIME_TYPES) {
            throw IllegalArgumentException("Formato de archivo no soportado por el backend. Use JPEG, PNG o WebP")
        }

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

    override suspend fun downloadEvidenceFile(id: String): ResponseBody {
        return executeApiCall {
            apiService.downloadEvidenceFile(id)
        }
    }
}
