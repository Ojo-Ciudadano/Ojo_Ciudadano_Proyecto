package cl.uct.ojociudadano.data.model

data class IncidentAssignment(
    val id: String,
    val assignedUserId: String,
    val assignedByUserId: String,
    val assignmentDate: String,
    val isActive: Boolean
)
