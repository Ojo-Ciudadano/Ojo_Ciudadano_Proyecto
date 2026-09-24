package cl.uct.ojociudadano.data.model

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
    val updatedAt: String
)
