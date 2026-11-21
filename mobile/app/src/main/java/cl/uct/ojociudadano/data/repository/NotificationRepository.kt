package cl.uct.ojociudadano.data.repository

import cl.uct.ojociudadano.data.model.Notification
import kotlinx.coroutines.flow.StateFlow

/**
 * Fuente observable de las notificaciones del usuario autenticado.
 *
 * La UI consume [notifications] y [unreadCount]; el refresco y el marcado de
 * lectura se serializan en la capa de datos para que varias pantallas o el
 * ciclo periódico no dupliquen ni pisen consultas.
 */
interface NotificationRepository {

    /** Última colección conocida, en el orden entregado por el backend. */
    val notifications: StateFlow<List<Notification>>

    /** Cantidad derivada de notificaciones no leídas. */
    val unreadCount: StateFlow<Int>

    /** Consulta el backend y publica el resultado. Nunca devuelve datos de otra sesión. */
    suspend fun refresh(): List<Notification>

    /** Marca una notificación como leída. Lanza si el backend rechaza la operación. */
    suspend fun markAsRead(id: String)

    /** Marca todas las pendientes y devuelve cuántas quedaron leídas. */
    suspend fun markAllAsRead(): Int

    /** Descarta el estado local; se usa al cerrar o cambiar de sesión. */
    fun clear()
}
