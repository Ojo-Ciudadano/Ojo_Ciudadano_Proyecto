package cl.uct.ojociudadano.data.repository

import cl.uct.ojociudadano.data.model.Paginated
import cl.uct.ojociudadano.data.model.UserProfile
import cl.uct.ojociudadano.data.session.UserRole

interface UserRepository {
    /** Un rol nulo omite el filtro. */
    suspend fun getUsers(page: Int, limit: Int, role: UserRole?): Paginated<UserProfile>

    /** Devuelve los roles confirmados por el servidor para la cuenta. */
    suspend fun updateUserRole(userId: String, role: UserRole): List<String>
}
