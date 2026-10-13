package cl.uct.ojociudadano.data.repository

import cl.uct.ojociudadano.data.session.AuthenticatedSession

interface AuthRepository {

    suspend fun login(
        email: String,
        password: String
    ): AuthenticatedSession
}
