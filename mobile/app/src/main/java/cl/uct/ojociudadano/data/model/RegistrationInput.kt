package cl.uct.ojociudadano.data.model

class RegistrationInput(
    val name: String,
    val email: String,
    val password: String,
    val phone: String = ""
) {
    override fun toString() = "RegistrationInput([redacted])"
}

data class RegisteredUser(val id: String, val name: String, val email: String)
