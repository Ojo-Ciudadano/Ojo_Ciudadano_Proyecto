package cl.uct.ojociudadano.data.model

data class IncidentStateHistory(
    val id: String,
    val incidenteId: String,
    val status: IncidentStatus,
    val userId: String,
    val observation: String?,
    val date: String
)
