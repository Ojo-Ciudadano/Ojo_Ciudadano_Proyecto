package cl.uct.ojociudadano.data.session

interface TokenProvider {

    fun currentToken(): String?
}
