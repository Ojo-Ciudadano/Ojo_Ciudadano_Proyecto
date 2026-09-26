package cl.uct.ojociudadano.di

import cl.uct.ojociudadano.BuildConfig
import cl.uct.ojociudadano.data.remote.ApiService
import cl.uct.ojociudadano.data.remote.RetrofitClient
import cl.uct.ojociudadano.data.repository.CategoryRepository
import cl.uct.ojociudadano.data.repository.IncidentRepository
import cl.uct.ojociudadano.data.repository.RemoteCategoryRepository
import cl.uct.ojociudadano.data.repository.RemoteIncidentRepository

class AppContainer(
    baseUrl: String = BuildConfig.API_BASE_URL
) {

    val apiService: ApiService by lazy {
        RetrofitClient.createApiService(baseUrl)
    }

    val categoryRepository: CategoryRepository by lazy {
        RemoteCategoryRepository(apiService)
    }

    val incidentRepository: IncidentRepository by lazy {
        RemoteIncidentRepository(apiService)
    }
}
