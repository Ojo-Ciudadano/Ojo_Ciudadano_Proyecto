package cl.uct.ojociudadano.data.model

import cl.uct.ojociudadano.domain.model.Evidence

data class Incident(
    val id: String,
    val title: String,
    val description: String,
    val category: Category,
    val status: IncidentStatus,
    val location: IncidentLocation,
    val reporterUserId: String,
    val priorityLevel: Int?,
    val createdAt: String,
    val updatedAt: String,
    val history: List<IncidentStateHistory> = emptyList(),
    val activeAssignment: IncidentAssignment? = null,
    val evidences: List<Evidence> = emptyList()
)
