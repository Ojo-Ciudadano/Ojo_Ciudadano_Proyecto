package cl.uct.ojociudadano.data.remote.dto

data class IncidentDto(
    val id: String,
    val titulo: String,
    val descripcion: String,
    val categoriaId: String,
    val estadoId: String,
    val ubicacionId: String,
    val usuarioReportaId: String,
    val nivelPrioridad: Int?,
    val fechaCreacion: String,
    val fechaActualizacion: String,
    val categoria: CategoryDto,
    val estado: IncidentStatusDto,
    val ubicacion: IncidentLocationDto
)
