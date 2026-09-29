package cl.uct.ojociudadano.data.session

data class AuthenticatedSession(
    val accessToken: String,
    val tokenType: String,
    val userId: String,
    val email: String,
    val name: String,
    val roles: List<String>
)
