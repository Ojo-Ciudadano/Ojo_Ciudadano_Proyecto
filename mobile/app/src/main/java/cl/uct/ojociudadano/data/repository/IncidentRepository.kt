package cl.uct.ojociudadano.data.repository

import cl.uct.ojociudadano.data.model.CreateIncidentInput
import cl.uct.ojociudadano.data.model.Incident
import cl.uct.ojociudadano.domain.model.Evidence
import okhttp3.ResponseBody
import java.io.File
import cl.uct.ojociudadano.data.model.IncidentStateHistory

interface IncidentRepository {

    suspend fun getIncidents(): List<Incident>

    suspend fun getCurrentUserIncidents(): List<Incident>

    suspend fun getIncidentById(id: String): Incident

    suspend fun createIncident(input: CreateIncidentInput): Incident

    suspend fun uploadEvidence(incidenteId: String, file: File, mimeType: String): Evidence

    suspend fun getEvidencesByIncident(incidenteId: String): List<Evidence>

    suspend fun getEvidenceById(id: String): Evidence

    suspend fun downloadEvidenceFile(id: String): ResponseBody
    suspend fun getIncidentHistory(incidentId: String): List<IncidentStateHistory>
    suspend fun assignResponsible(incidentId: String, assignedUserId: String): Incident
    suspend fun updatePriority(incidentId: String, priorityLevel: Int): Incident
}
