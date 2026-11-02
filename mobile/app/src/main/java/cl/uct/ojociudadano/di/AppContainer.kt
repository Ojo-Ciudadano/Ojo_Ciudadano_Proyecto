package cl.uct.ojociudadano.di

import android.content.Context
import cl.uct.ojociudadano.BuildConfig
import cl.uct.ojociudadano.data.evidence.EvidenceImageFetcher
import cl.uct.ojociudadano.data.evidence.EvidenceImageKeyer
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
import coil3.ImageLoader

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
            tokenProvider = sessionManager,
            onUnauthorized = sessionManager::invalidateIfCurrent
        )
    }

    val categoryRepository: CategoryRepository by lazy {
        RemoteCategoryRepository(apiService)
    }

    val incidentRepository: IncidentRepository by lazy {
        RemoteIncidentRepository(apiService, sessionManager)
    }

    /**
     * Coil descarga las evidencias mediante el repositorio y mantiene los
     * resultados solo en memoria. El disco se desactiva para no persistir
     * fotografías autenticadas fuera de la sesión.
     */
    val imageLoader: ImageLoader by lazy {
        ImageLoader.Builder(context.applicationContext)
            .components {
                add(EvidenceImageFetcher.Factory(incidentRepository))
                add(EvidenceImageKeyer())
            }
            .diskCache(null)
            .build()
    }
}
