package cl.uct.ojociudadano.data.repository

import cl.uct.ojociudadano.data.mapper.toAuthenticatedSession
import cl.uct.ojociudadano.data.remote.ApiService
import cl.uct.ojociudadano.data.remote.dto.LoginRequestDto
import cl.uct.ojociudadano.data.remote.error.executeApiCall
import cl.uct.ojociudadano.data.session.AuthenticatedSession
import cl.uct.ojociudadano.data.session.SessionStorage

class RemoteAuthRepository(
    private val apiService: ApiService,
    private val sessionStorage: SessionStorage
) : AuthRepository {

    override suspend fun login(
        email: String,
        password: String
    ): AuthenticatedSession {
        val response = executeApiCall {
            apiService.login(LoginRequestDto(email, password))
        }
        val session = response.toAuthenticatedSession()

        sessionStorage.save(session)

        return session
    }
}
