package cl.uct.ojociudadano.data.mapper

import cl.uct.ojociudadano.data.model.Notification
import cl.uct.ojociudadano.data.remote.dto.NotificationDto

fun NotificationDto.toDomain(): Notification {
    return Notification(
        id = id,
        userId = usuarioId,
        incidentId = incidenteId,
        message = mensaje,
        isRead = leido,
        createdAt = fecha
    )
}
