package cl.uct.ojociudadano.di

import android.content.Context
import androidx.lifecycle.ProcessLifecycleOwner
import cl.uct.ojociudadano.BuildConfig
import cl.uct.ojociudadano.data.evidence.EvidenceImageFetcher
import cl.uct.ojociudadano.data.evidence.EvidenceImageKeyer
import cl.uct.ojociudadano.data.evidence.SessionImageCacheInvalidator
import cl.uct.ojociudadano.data.location.AndroidGeocoderReverseGeocodingClient
import cl.uct.ojociudadano.data.location.DefaultLocationClient
import cl.uct.ojociudadano.data.media.AndroidEvidenceImageCompressor
import cl.uct.ojociudadano.data.media.CacheEvidencePhotoFileStore
import cl.uct.ojociudadano.data.media.EvidenceImageCompressor
import cl.uct.ojociudadano.data.media.EvidencePhotoFileStore
import cl.uct.ojociudadano.data.media.EvidenceTempCleaner
import cl.uct.ojociudadano.data.notifications.AppForegroundState
import cl.uct.ojociudadano.data.notifications.NotificationSyncScheduler
import cl.uct.ojociudadano.data.remote.ApiService
import cl.uct.ojociudadano.data.remote.RetrofitClient
import cl.uct.ojociudadano.data.repository.UserRepository
import cl.uct.ojociudadano.data.repository.AuthRepository
import cl.uct.ojociudadano.data.repository.CategoryRepository
import cl.uct.ojociudadano.data.repository.IncidentRepository
import cl.uct.ojociudadano.data.repository.NotificationRepository
import cl.uct.ojociudadano.data.repository.ProfileRepository
import cl.uct.ojociudadano.data.repository.RemoteAuthRepository
import cl.uct.ojociudadano.data.repository.RemoteCategoryRepository
import cl.uct.ojociudadano.data.repository.RemoteIncidentRepository
import cl.uct.ojociudadano.data.repository.RemoteNotificationRepository
import cl.uct.ojociudadano.data.repository.RemoteProfileRepository
import cl.uct.ojociudadano.data.session.DefaultSessionManager
import cl.uct.ojociudadano.data.session.EncryptedSharedPreferencesSessionStorage
import cl.uct.ojociudadano.data.session.SessionManager
import cl.uct.ojociudadano.data.session.SessionStorage
import cl.uct.ojociudadano.domain.repository.LocationClient
import cl.uct.ojociudadano.domain.repository.ReverseGeocodingClient
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import com.google.android.gms.location.LocationServices
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okio.Path.Companion.toOkioPath

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

    /**
     * Alcance del proceso para trabajo que debe sobrevivir a una pantalla
     * (estado reactivo de notificaciones y su ciclo de sincronización).
     */
    val applicationScope: CoroutineScope by lazy {
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    val evidencePhotoStore: EvidencePhotoFileStore by lazy {
        CacheEvidencePhotoFileStore(context.applicationContext)
    }

    /**
     * Fuente única de ubicación del dispositivo. Los ViewModels consumen esta
     * abstracción y nunca acceden directamente a los servicios de Google Play.
     */
    val locationClient: LocationClient by lazy {
        DefaultLocationClient(
            context = context.applicationContext,
            client = LocationServices.getFusedLocationProviderClient(
                context.applicationContext
            )
        )
    }

    /**
     * Servicio desacoplado que completa comuna, región y dirección referencial
     * a partir de las coordenadas obtenidas por GPS o fijadas manualmente.
     */
    val reverseGeocodingClient: ReverseGeocodingClient by lazy {
        AndroidGeocoderReverseGeocodingClient(context.applicationContext)
    }

    val apiService: ApiService by lazy {
        RetrofitClient.createApiService(
            baseUrl = baseUrl,
            tokenProvider = sessionManager
        )
    }

    val categoryRepository: CategoryRepository by lazy {
        RemoteCategoryRepository(apiService)
    }

    /**
     * Reduce y recomprime las evidencias antes de enviarlas (#277). El
     * directorio temporal vive dentro de la caché de la app y cada archivo se
     * borra al terminar la petición.
     */
    val evidenceImageCompressor: EvidenceImageCompressor by lazy {
        AndroidEvidenceImageCompressor(
            outputDirectory = File(
                context.applicationContext.cacheDir,
                EvidenceTempCleaner.UPLOADS_DIRECTORY_NAME
            )
        )
    }

    /**
     * Barre los temporales de evidencias al arrancar la app (#280): fotos de
     * cámara o galería del formulario y copias de compresión que quedaron tras
     * un cierre abrupto o la muerte del proceso.
     */
    val evidenceTempCleaner: EvidenceTempCleaner by lazy {
        EvidenceTempCleaner(
            evidencesDirectory = File(
                context.applicationContext.cacheDir,
                CacheEvidencePhotoFileStore.EVIDENCE_DIRECTORY_NAME
            ),
            uploadsDirectory = File(
                context.applicationContext.cacheDir,
                EvidenceTempCleaner.UPLOADS_DIRECTORY_NAME
            )
        )
    }

    /**
     * Vacía la caché de imágenes cuando cambia la cuenta autenticada (#280),
     * para no reutilizar evidencias decodificadas de otra sesión.
     */
    val imageCacheInvalidator: SessionImageCacheInvalidator by lazy {
        SessionImageCacheInvalidator(
            clearMemoryCache = { imageLoader.memoryCache?.clear() },
            clearDiskCache = { imageLoader.diskCache?.clear() }
        )
    }

    private val remoteIncidentRepository by lazy {
        RemoteIncidentRepository(
            apiService = apiService,
            sessionManager = sessionManager,
            evidenceImageCompressor = evidenceImageCompressor
        )
    }

    val incidentRepository: IncidentRepository get() = remoteIncidentRepository

    val userRepository: UserRepository get() = remoteIncidentRepository

    val notificationRepository: NotificationRepository by lazy {
        RemoteNotificationRepository(apiService, sessionManager, applicationScope)
    }

    val appForegroundState: AppForegroundState by lazy {
        AppForegroundState(ProcessLifecycleOwner.get().lifecycle)
    }

    val notificationSyncScheduler: NotificationSyncScheduler by lazy {
        NotificationSyncScheduler(
            repository = notificationRepository,
            sessionManager = sessionManager,
            foreground = appForegroundState.isForeground,
            scope = applicationScope
        )
    }

    val profileRepository: ProfileRepository by lazy {
        RemoteProfileRepository(apiService, sessionManager)
    }

    /**
     * Coil descarga las evidencias mediante el repositorio y las mantiene en
     * memoria y en un caché de disco acotado (#280). La caché se invalida al
     * cambiar de cuenta y la clave incluye el alcance de la sesión, de modo que
     * las fotografías autenticadas no se mezclan entre usuarios.
     */
    val imageLoader: ImageLoader by lazy {
        val appContext = context.applicationContext

        ImageLoader.Builder(appContext)
            .components {
                add(EvidenceImageFetcher.Factory(incidentRepository))
                add(
                    EvidenceImageKeyer {
                        SessionImageCacheInvalidator.scopeOf(sessionManager.state.value).orEmpty()
                    }
                )
            }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(appContext, MEMORY_CACHE_PERCENT)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(
                        File(appContext.cacheDir, IMAGE_CACHE_DIRECTORY_NAME).toOkioPath()
                    )
                    .maxSizeBytes(IMAGE_CACHE_MAX_BYTES)
                    .build()
            }
            .build()
    }

    private companion object {
        /** Porcentaje de la memoria de la app reservado para miniaturas decodificadas. */
        const val MEMORY_CACHE_PERCENT = 0.2

        const val IMAGE_CACHE_DIRECTORY_NAME = "image-cache"

        /** Tamaño máximo del caché de disco de imágenes. */
        const val IMAGE_CACHE_MAX_BYTES = 25L * 1024L * 1024L
    }
}
