package cl.uct.ojociudadano.data.remote.dto

data class AsignacionDto(
    val id: String,
    val usuarioAsignadoId: String,
    val asignadoPorId: String,
    val fechaAsignacion: String,
    val activa: Boolean
)
