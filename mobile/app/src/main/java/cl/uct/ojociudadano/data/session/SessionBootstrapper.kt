package cl.uct.ojociudadano.data.session

import cl.uct.ojociudadano.data.repository.AuthRepository
import java.util.concurrent.CancellationException

class SessionBootstrapper(
    private val sessionStorage: SessionStorage,
    private val authRepository: AuthRepository,
    private val developmentCredentials: DevelopmentAuthCredentials
) {

    suspend fun bootstrap(): SessionBootstrapState {
        return try {
            val existingSession = sessionStorage.currentSession()

            if (existingSession != null && !existingSession.isExpired()) {
                SessionBootstrapState.Authenticated(existingSession)
            } else {
                if (existingSession != null) {
                    sessionStorage.clear()
                }
                loginWithDevelopmentCredentials()
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            SessionBootstrapState.Error(exception)
        }
    }

    private suspend fun loginWithDevelopmentCredentials(): SessionBootstrapState {
        check(developmentCredentials.isConfigured()) {
            "Falta configurar DEV_AUTH_EMAIL y DEV_AUTH_PASSWORD para iniciar la sesión de desarrollo"
        }

        val session = authRepository.login(
            email = developmentCredentials.email,
            password = developmentCredentials.password
        )

        return SessionBootstrapState.Authenticated(session)
    }
}

sealed interface SessionBootstrapState {

    data object Loading : SessionBootstrapState

    data class Authenticated(
        val session: AuthenticatedSession
    ) : SessionBootstrapState

    data class Error(
        val cause: Throwable
    ) : SessionBootstrapState
}
