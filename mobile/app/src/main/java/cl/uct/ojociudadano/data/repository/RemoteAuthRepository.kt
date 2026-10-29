package cl.uct.ojociudadano.data.repository

import cl.uct.ojociudadano.data.mapper.toAuthenticatedSession
import cl.uct.ojociudadano.data.remote.ApiService
import cl.uct.ojociudadano.data.remote.dto.LoginRequestDto
import cl.uct.ojociudadano.data.remote.error.executeApiCall
import cl.uct.ojociudadano.data.session.AuthenticatedSession
import cl.uct.ojociudadano.data.model.RegisteredUser
import cl.uct.ojociudadano.data.model.RegistrationInput
import cl.uct.ojociudadano.data.remote.dto.RegisterRequestDto
import com.google.gson.stream.MalformedJsonException

class RemoteAuthRepository(
    private val apiService: ApiService
) : AuthRepository {

    override suspend fun register(
        input: RegistrationInput
    ): RegisteredUser = executeApiCall {
        val response = try {
            apiService.register(
                RegisterRequestDto(
                    nombre = input.name.trim(),
                    email = input.email.trim(),
                    password = input.password,
                    telefono = input.phone.trim().takeIf { it.isNotEmpty() }
                )
            )
        } catch (exception: MalformedJsonException) {
            throw IllegalStateException("Respuesta de registro inválida", exception)
        }
        RegisteredUser(
            id = requireNotNull(response.id?.takeIf { it.isNotBlank() }),
            name = requireNotNull(response.nombre?.takeIf { it.isNotBlank() }),
            email = requireNotNull(response.email?.takeIf { it.isNotBlank() })
        )
    }

    override suspend fun login(
        email: String,
        password: String
    ): AuthenticatedSession {
        val response = executeApiCall {
            apiService.login(LoginRequestDto(email, password))
        }
        return response.toAuthenticatedSession()
    }
}
