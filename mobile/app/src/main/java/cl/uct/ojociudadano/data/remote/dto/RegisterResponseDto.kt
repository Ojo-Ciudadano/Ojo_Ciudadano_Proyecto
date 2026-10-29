package cl.uct.ojociudadano.data.remote.dto

/** Registro devuelve un usuario directamente, sin token ni envoltorio usuario. */
data class RegisterResponseDto(
    val id: String? = null,
    val nombre: String? = null,
    val email: String? = null,
    val telefono: String? = null,
    val fechaRegistro: String? = null
)
