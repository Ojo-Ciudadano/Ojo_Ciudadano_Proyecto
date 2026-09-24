package cl.uct.ojociudadano.data.model

data class Category(
    val id: String,
    val name: String,
    val description: String,
    val basePriority: Int,
    val isCriticalEmergency: Boolean,
    val officialContactChannel: String?
)
