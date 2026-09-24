package cl.uct.ojociudadano.data.repository

import cl.uct.ojociudadano.data.model.Category

interface CategoryRepository {

    suspend fun getCategories(): List<Category>
}
