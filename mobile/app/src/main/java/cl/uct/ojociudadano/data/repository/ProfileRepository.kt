package cl.uct.ojociudadano.data.repository

import cl.uct.ojociudadano.data.model.UserProfile

/**
 * Acceso al perfil del usuario autenticado. La identidad (y por lo tanto el
 * JWT) la resuelve la implementación a partir de la sesión vigente: la UI no
 * puede pedir el perfil de otro usuario.
 */
interface ProfileRepository {
    suspend fun getProfile(): UserProfile
}
