package cl.uct.ojociudadano.data.session

interface SessionStorage : TokenProvider {

    fun currentSession(): AuthenticatedSession?

    fun save(session: AuthenticatedSession)

    fun clear()
}
