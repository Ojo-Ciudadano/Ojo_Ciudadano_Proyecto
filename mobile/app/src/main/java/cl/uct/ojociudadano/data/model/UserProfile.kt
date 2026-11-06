package cl.uct.ojociudadano.data.model

/**
 * Perfil del usuario autenticado tal como lo entrega `GET /api/v1/usuarios/me`.
 *
 * `phone` y `registeredAt` son opcionales porque el contrato admite teléfono
 * nulo. La UI nunca inventa valores: cuando faltan se muestra un texto
 * explícito de ausencia.
 */
data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val phone: String?,
    val registeredAt: String?,
    val roles: List<String>
)
