package cl.uct.ojociudadano.data.session

interface TokenProvider {

    fun currentToken(): String?

    fun currentTokenSnapshot(): TokenSnapshot? {
        return currentToken()?.let(::TokenSnapshot)
    }
}

data class TokenSnapshot(
    val value: String,
    val sessionGeneration: Long? = null
) {
    override fun toString(): String {
        return "TokenSnapshot(value=<redacted>, sessionGeneration=$sessionGeneration)"
    }
}
