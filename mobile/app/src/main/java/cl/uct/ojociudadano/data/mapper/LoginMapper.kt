package cl.uct.ojociudadano.data.mapper

import cl.uct.ojociudadano.data.remote.dto.LoginResponseDto
import cl.uct.ojociudadano.data.session.AuthenticatedSession

fun LoginResponseDto.toAuthenticatedSession(): AuthenticatedSession {
    val user = authenticatedUser()
    val roles = user.roles.orEmpty().toList()
    require(roles.all { !it.isNullOrBlank() }) {
        "La respuesta de login contiene roles inválidos"
    }

    return AuthenticatedSession(
        accessToken = access_token.requiredAuthField("access_token"),
        tokenType = token_type.requiredAuthField("token_type"),
        userId = user.id.requiredAuthField("usuario.id"),
        email = user.email.requiredAuthField("usuario.email"),
        name = user.nombre.requiredAuthField("usuario.nombre"),
        roles = roles
    )
}

private fun String?.requiredAuthField(field: String): String =
    requireNotNull(this?.takeIf { it.isNotBlank() }) {
        "La respuesta de login no contiene un campo válido: $field"
    }
