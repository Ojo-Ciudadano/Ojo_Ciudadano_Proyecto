package cl.uct.ojociudadano.data.repository

import cl.uct.ojociudadano.data.model.Paginated
import cl.uct.ojociudadano.data.model.UserProfile
import cl.uct.ojociudadano.data.session.UserRole

/** Consulta administrativa; null omite el filtro y recupera ambos roles. */
fun interface UserRepository {
    suspend fun getUsers(page: Int, limit: Int, role: UserRole?): Paginated<UserProfile>
}
