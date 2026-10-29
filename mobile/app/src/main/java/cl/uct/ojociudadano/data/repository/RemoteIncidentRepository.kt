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
import cl.uct.ojociudadano.domain.model.Evidence
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
import java.io.File

class RemoteIncidentRepository(
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) : IncidentRepository {

    companion object {
        private const val MAX_BACKEND_FILE_SIZE = 10L * 1024L * 1024L
        private val ALLOWED_MIME_TYPES = setOf("image/jpeg", "image/png", "image/webp")
    }

    override suspend fun getIncidents(): List<Incident> {
        val response = withCurrentIdentity { identity ->
            apiService.getIncidents(expectedSession = identity.token)
        }
        return response.map { it.toDomain() }
    }

    override suspend fun getCurrentUserIncidents(): List<Incident> {
        return withCurrentIdentity { identity ->
            // Defense in depth: never trust a server that accidentally ignores the query filter.
            apiService.getIncidents(identity.userId, identity.token)
                .map { it.toDomain() }
                .filter { it.reporterUserId == identity.userId }
        }
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
        if (runCatching { sessionManager.requireIdentity() }.getOrNull() != identity) {
            // Streaming downloads own a connection that must be released on a stale response.
            if (result is ResponseBody) result.close()
            throw SessionRequiredException()
        }
        return result
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

        val response = withCurrentIdentity { identity ->
            apiService.uploadEvidence(incidenteId, body, identity.token)
        }
        return response.toDomain()
    }

    override suspend fun getEvidencesByIncident(incidenteId: String): List<Evidence> {
        val response = withCurrentIdentity { identity ->
            apiService.getEvidencesByIncident(incidenteId, identity.token)
        }
        return response.map { it.toDomain() }
    }

    override suspend fun getEvidenceById(id: String): Evidence {
        val response = withCurrentIdentity { identity ->
            apiService.getEvidenceById(id, identity.token)
        }
        return response.toDomain()
    }

    override suspend fun downloadEvidenceFile(id: String): ResponseBody {
        return withCurrentIdentity { identity ->
            apiService.downloadEvidenceFile(id, identity.token)
        }
    }
}
