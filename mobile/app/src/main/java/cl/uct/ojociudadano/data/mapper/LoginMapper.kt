package cl.uct.ojociudadano.data.mapper

import cl.uct.ojociudadano.data.remote.dto.LoginResponseDto
import cl.uct.ojociudadano.data.session.AuthenticatedSession

fun LoginResponseDto.toAuthenticatedSession(): AuthenticatedSession {
    val user = authenticatedUser()

    return AuthenticatedSession(
        accessToken = access_token,
        tokenType = token_type,
        userId = user.id,
        email = user.email,
        name = user.nombre,
        roles = user.roles
    )
}
