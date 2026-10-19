package cl.uct.ojociudadano.di

import android.content.Context
import cl.uct.ojociudadano.BuildConfig
import cl.uct.ojociudadano.data.remote.ApiService
import cl.uct.ojociudadano.data.remote.RetrofitClient
import cl.uct.ojociudadano.data.repository.AuthRepository
import cl.uct.ojociudadano.data.repository.CategoryRepository
import cl.uct.ojociudadano.data.repository.IncidentRepository
import cl.uct.ojociudadano.data.repository.RemoteAuthRepository
import cl.uct.ojociudadano.data.repository.RemoteCategoryRepository
import cl.uct.ojociudadano.data.repository.RemoteIncidentRepository
import cl.uct.ojociudadano.data.session.SessionBootstrapper
import cl.uct.ojociudadano.data.session.SessionStorage
import cl.uct.ojociudadano.data.session.SharedPreferencesSessionStorage

class AppContainer(
    context: Context,
    baseUrl: String = BuildConfig.API_BASE_URL
) {
    val sessionStorage: SessionStorage by lazy {
        SharedPreferencesSessionStorage(
            context = context,
            environmentBaseUrl = baseUrl
        )
    }

    private val authApiService: ApiService by lazy {
        RetrofitClient.createApiService(baseUrl = baseUrl)
    }

    val authRepository: AuthRepository by lazy {
        RemoteAuthRepository(authApiService, sessionStorage)
    }

    val sessionBootstrapper: SessionBootstrapper by lazy {
        SessionBootstrapper(
            sessionStorage = sessionStorage,
            authRepository = authRepository
        )
    }

    val apiService: ApiService by lazy {
        RetrofitClient.createApiService(
            baseUrl = baseUrl,
            tokenProvider = sessionStorage,
            onUnauthorizedRefreshToken = {
                sessionBootstrapper.onSessionInvalidated()
                null
            }
        )
    }

    val categoryRepository: CategoryRepository by lazy {
        RemoteCategoryRepository(apiService)
    }

    val incidentRepository: IncidentRepository by lazy {
        RemoteIncidentRepository(apiService)
    }
}
