package cl.uct.ojociudadano.data.repository

import cl.uct.ojociudadano.data.mapper.toDomain
import cl.uct.ojociudadano.data.model.Category
import cl.uct.ojociudadano.data.remote.ApiService

class RemoteCategoryRepository(
    private val apiService: ApiService
) : CategoryRepository {

    override suspend fun getCategories(): List<Category> {
        return apiService
            .getCategories()
            .map { it.toDomain() }
    }
}
