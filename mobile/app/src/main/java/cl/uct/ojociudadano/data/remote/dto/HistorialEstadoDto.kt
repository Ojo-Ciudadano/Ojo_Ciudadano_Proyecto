package cl.uct.ojociudadano.data.remote.dto

data class HistorialEstadoDto(
    val id: String,
    val incidenteId: String,
    val estado: IncidentStatusDto,
    val usuarioId: String,
    val observacion: String?,
    val fecha: String
)
