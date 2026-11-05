package cl.uct.ojociudadano.data.remote.dto

data class NotificationDto(
    val id: String,
    val usuarioId: String,
    val incidenteId: String,
    val mensaje: String,
    val leido: Boolean,
    val fecha: String
)
