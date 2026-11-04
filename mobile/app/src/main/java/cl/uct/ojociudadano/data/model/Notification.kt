package cl.uct.ojociudadano.data.model

data class Notification(
    val id: String,
    val userId: String,
    val incidentId: String,
    val message: String,
    val isRead: Boolean,
    val createdAt: String
)
