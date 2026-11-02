package cl.uct.ojociudadano.data.remote.dto

data class AuthUserDto(
    val id: String? = null,
    val nombre: String? = null,
    val email: String? = null,
    val roles: List<String>? = null
)
