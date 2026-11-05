package cl.uct.ojociudadano.data.repository

import cl.uct.ojociudadano.data.model.IncidentStatusTransitions
import cl.uct.ojociudadano.data.remote.dto.UpdateIncidentStatusRequestDto
import cl.uct.ojociudadano.data.remote.error.ApiException
import cl.uct.ojociudadano.data.session.SessionState
import cl.uct.ojociudadano.data.session.SessionRoles
import cl.uct.ojociudadano.data.mapper.toCandidatePage
import cl.uct.ojociudadano.data.mapper.toDomain
import cl.uct.ojociudadano.data.mapper.toRequestDto
import cl.uct.ojociudadano.data.model.CandidateUser
import cl.uct.ojociudadano.data.model.CreateIncidentInput
import cl.uct.ojociudadano.data.model.EvidenceUploadConstraints
import cl.uct.ojociudadano.data.model.Incident
import cl.uct.ojociudadano.data.model.IncidentAssignment
import cl.uct.ojociudadano.data.model.IncidentFilters
import cl.uct.ojociudadano.data.model.IncidentStateHistory
import cl.uct.ojociudadano.data.model.Paginated
import cl.uct.ojociudadano.data.remote.ApiService
import cl.uct.ojociudadano.data.remote.dto.AssignResponsibleRequestDto
import cl.uct.ojociudadano.data.remote.dto.UpdatePriorityRequestDto
import cl.uct.ojociudadano.data.remote.error.executeApiCall
import cl.uct.ojociudadano.data.session.SessionIdentity
import cl.uct.ojociudadano.data.session.SessionManager
import cl.uct.ojociudadano.data.session.SessionRequiredException
import cl.uct.ojociudadano.data.session.UserRole
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

    override suspend fun updateStatus(incidentId: String, status: String, observation: String): Incident {
        require(IncidentStatusTransitions.isCanonical(status))
        require(observation.isNotBlank())
        val response = withCurrentIdentity(requiredRole = UserRole.ADMIN) { identity ->
            apiService.updateIncidentStatus(
                incidentId,
                UpdateIncidentStatusRequestDto(status, observation.trim()),
                identity.token
            )
        }
        return response.toDomain()
    }

    override suspend fun getIncidents(filters: IncidentFilters): List<Incident> {
        val response = withCurrentIdentity { identity ->
            apiService.getIncidents(
                estadoId = filters.estadoId,
                categoriaId = filters.categoriaId,
                comuna = filters.comuna,
                expectedSession = identity.token
            )
        }
        return response.map { it.toDomain() }
    }

    override suspend fun getCurrentUserIncidents(): List<Incident> {
        return withCurrentIdentity { identity ->
            // Defense in depth: never trust a server that accidentally ignores the query filter.
            apiService.getIncidents(
                usuarioReportaId = identity.userId,
                expectedSession = identity.token
            )
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

    private suspend fun <T> withCurrentIdentity(
        requiredRole: UserRole? = null,
        block: suspend (SessionIdentity) -> T
    ): T {
        val identity = sessionManager.requireIdentity()
        if (requiredRole != null) {
            val session = (sessionManager.state.value as? SessionState.Authenticated)?.session
            val roles = session?.roleState as? SessionRoles.Known
            if (roles?.values?.contains(requiredRole) != true) {
                throw ApiException.Http(403, emptyList(), null, null)
            }
            if (sessionManager.requireIdentity() != identity) throw SessionRequiredException()
        }
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
        if (file.length() > EvidenceUploadConstraints.MAX_FILE_SIZE_BYTES) {
            throw IllegalArgumentException("El archivo supera el límite máximo de 10 MB aceptado por el backend")
        }
        if (!EvidenceUploadConstraints.isSupportedMimeType(mimeType)) {
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

    override suspend fun getIncidentHistory(incidentId: String): List<IncidentStateHistory> {
        val response = withCurrentIdentity { identity ->
            apiService.getIncidentHistory(incidentId, identity.token)
        }
        return response.map { it.toDomain() }
    }

    override suspend fun assignResponsible(
        incidentId: String,
        assignedUserId: String
    ): IncidentAssignment {
        val request = AssignResponsibleRequestDto(usuarioAsignadoId = assignedUserId)
        val response = withCurrentIdentity { identity ->
            apiService.assignResponsible(incidentId, request, identity.token)
        }
        return response.toDomain()
    }

    override suspend fun updatePriority(incidentId: String, priorityLevel: Int): Incident {
        val request = UpdatePriorityRequestDto(nivelPrioridad = priorityLevel)
        val response = withCurrentIdentity { identity ->
            apiService.updatePriority(incidentId, request, identity.token)
        }
        return response.toDomain()
    }

    override suspend fun getCandidates(page: Int?, limit: Int?): Paginated<CandidateUser> {
        val response = withCurrentIdentity { identity ->
            apiService.getUsuarios(
                page = page,
                limit = limit,
                rol = UserRole.ADMIN.backendValue,
                expectedSession = identity.token
            )
        }
        return response.toCandidatePage()
    }
}
