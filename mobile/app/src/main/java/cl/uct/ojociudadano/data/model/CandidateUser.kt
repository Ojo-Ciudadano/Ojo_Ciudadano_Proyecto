package cl.uct.ojociudadano.data.model

/** Usuario habilitado para ser asignado como responsable de un incidente. */
data class CandidateUser(
    val id: String,
    val name: String,
    val email: String,
    val roles: List<String>
)
