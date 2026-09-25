package cl.uct.ojociudadano.data.repository

import cl.uct.ojociudadano.data.mapper.toDomain
import cl.uct.ojociudadano.data.model.Category
import cl.uct.ojociudadano.data.remote.ApiService
import cl.uct.ojociudadano.data.remote.error.executeApiCall

class RemoteCategoryRepository(
    private val apiService: ApiService
) : CategoryRepository {

    override suspend fun getCategories(): List<Category> {
        val response = executeApiCall {
            apiService.getCategories()
        }

        return response.map { it.toDomain() }
    }
}
