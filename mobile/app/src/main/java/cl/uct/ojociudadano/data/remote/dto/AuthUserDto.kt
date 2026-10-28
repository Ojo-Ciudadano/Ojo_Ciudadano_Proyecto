package cl.uct.ojociudadano.data.remote.dto

data class AuthUserDto(
    val id: String,
    val nombre: String,
    val email: String,
    val roles: List<String>? = null
)
