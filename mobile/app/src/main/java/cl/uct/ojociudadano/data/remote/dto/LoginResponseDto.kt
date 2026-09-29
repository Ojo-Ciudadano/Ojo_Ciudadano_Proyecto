package cl.uct.ojociudadano.data.remote.dto

data class LoginResponseDto(
    val access_token: String,
    val token_type: String,
    val usuario: AuthUserDto? = null,
    val user: AuthUserDto? = null
) {
    fun authenticatedUser(): AuthUserDto {
        return usuario ?: user
            ?: throw IllegalStateException("La respuesta de login no contiene un usuario")
    }
}
