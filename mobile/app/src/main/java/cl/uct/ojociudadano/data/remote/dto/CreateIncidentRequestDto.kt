package cl.uct.ojociudadano.data.remote.dto

data class CreateIncidentRequestDto(
    val titulo: String,
    val descripcion: String,
    val categoriaId: String,
    val usuarioReportaId: String,
    val ubicacion: LocationRequestDto
)
