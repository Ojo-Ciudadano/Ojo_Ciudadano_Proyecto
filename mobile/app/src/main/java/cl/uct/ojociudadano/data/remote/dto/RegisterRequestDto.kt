package cl.uct.ojociudadano.data.remote.dto

class RegisterRequestDto(
    val nombre: String,
    val email: String,
    val password: String,
    val telefono: String? = null
) {
    override fun toString() = "RegisterRequestDto([redacted])"
}
