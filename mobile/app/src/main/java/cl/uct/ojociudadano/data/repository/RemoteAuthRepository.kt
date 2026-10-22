package cl.uct.ojociudadano.data.repository

import cl.uct.ojociudadano.data.mapper.toAuthenticatedSession
import cl.uct.ojociudadano.data.remote.ApiService
import cl.uct.ojociudadano.data.remote.dto.LoginRequestDto
import cl.uct.ojociudadano.data.remote.error.executeApiCall
import cl.uct.ojociudadano.data.session.AuthenticatedSession

class RemoteAuthRepository(
    private val apiService: ApiService
) : AuthRepository {

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
