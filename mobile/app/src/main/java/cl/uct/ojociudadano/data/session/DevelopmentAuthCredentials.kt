package cl.uct.ojociudadano.data.session

data class DevelopmentAuthCredentials(
    val email: String,
    val password: String
) {

    fun isConfigured(): Boolean {
        return email.isNotBlank() && password.isNotBlank()
    }
}
