package cl.uct.ojociudadano.data.remote.dto

data class CategoryDto(
    val id: String,
    val nombre: String,
    val descripcion: String,
    val prioridadBase: Int,
    val esEmergenciaCritica: Boolean,
    val canalOficialContacto: String?
)
