package cl.uct.ojociudadano.data.remote.dto

data class UsuarioDto(
    val id: String,
    val nombre: String,
    val email: String,
    val telefono: String? = null,
    val fechaRegistro: String? = null,
    val roles: List<String>
)
