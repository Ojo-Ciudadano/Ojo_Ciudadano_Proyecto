package cl.uct.ojociudadano.di

import android.content.Context
import cl.uct.ojociudadano.BuildConfig
import cl.uct.ojociudadano.data.media.CacheEvidencePhotoFileStore
import cl.uct.ojociudadano.data.media.EvidencePhotoFileStore
import cl.uct.ojociudadano.data.remote.ApiService
import cl.uct.ojociudadano.data.remote.RetrofitClient
import cl.uct.ojociudadano.data.repository.AuthRepository
import cl.uct.ojociudadano.data.repository.CategoryRepository
import cl.uct.ojociudadano.data.repository.IncidentRepository
import cl.uct.ojociudadano.data.repository.RemoteAuthRepository
import cl.uct.ojociudadano.data.repository.RemoteCategoryRepository
import cl.uct.ojociudadano.data.repository.RemoteIncidentRepository
import cl.uct.ojociudadano.data.session.DefaultSessionManager
import cl.uct.ojociudadano.data.session.EncryptedSharedPreferencesSessionStorage
import cl.uct.ojociudadano.data.session.SessionManager
import cl.uct.ojociudadano.data.session.SessionStorage

class AppContainer(
    context: Context,
    baseUrl: String = BuildConfig.API_BASE_URL
) {
    val sessionStorage: SessionStorage by lazy {
        EncryptedSharedPreferencesSessionStorage(
            context = context,
            environmentBaseUrl = baseUrl
        )
    }

    private val authApiService: ApiService by lazy {
        RetrofitClient.createApiService(baseUrl = baseUrl)
    }

    val authRepository: AuthRepository by lazy {
        RemoteAuthRepository(authApiService)
    }

    val sessionManager: SessionManager by lazy {
        DefaultSessionManager(sessionStorage)
    }

    val evidencePhotoStore: EvidencePhotoFileStore by lazy {
        CacheEvidencePhotoFileStore(context.applicationContext)
    }

    val apiService: ApiService by lazy {
        RetrofitClient.createApiService(
            baseUrl = baseUrl,
            tokenProvider = sessionStorage,
            onUnauthorizedRefreshToken = {
                sessionManager.onStoredSessionInvalidated()
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
