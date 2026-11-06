package cl.uct.ojociudadano.data.repository

import cl.uct.ojociudadano.data.session.AuthenticatedSession
import cl.uct.ojociudadano.data.model.RegisteredUser
import cl.uct.ojociudadano.data.model.RegistrationInput

interface AuthRepository {

    suspend fun register(
        input: RegistrationInput
    ): RegisteredUser


    suspend fun login(
        email: String,
        password: String
    ): AuthenticatedSession
}
