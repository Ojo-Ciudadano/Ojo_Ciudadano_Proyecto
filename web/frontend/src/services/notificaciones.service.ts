
import { apiClient } from './api'
import type {
  Notificacion,
  NotificacionDto,
} from '../types/notificacion'

export function mapearNotificacion(
  dato: NotificacionDto,
): Notificacion {
  if (
    !dato ||
    typeof dato.id !== 'string' ||
    typeof dato.usuarioId !== 'string' ||
    typeof dato.incidenteId !== 'string' ||
    typeof dato.mensaje !== 'string' ||
    typeof dato.leido !== 'boolean' ||
    typeof dato.fecha !== 'string' ||
    !Number.isFinite(Date.parse(dato.fecha))
  ) {
    throw new Error(
      'El servidor devolvió una notificación inválida.',
    )
  }

  return {
    id: dato.id,
    usuarioId: dato.usuarioId,
    incidenteId: dato.incidenteId,
    mensaje: dato.mensaje,
    leido: dato.leido,
    fecha: dato.fecha,
  }
}

export async function obtenerNotificaciones(
  signal?: AbortSignal,
): Promise<Notificacion[]> {
  const response = await apiClient.get<unknown>(
    '/notificaciones',
    { signal },
  )

  if (!Array.isArray(response.data)) {
    throw new Error(
      'El servidor devolvió una lista de notificaciones inválida.',
    )
  }


  return response.data.map(
    (dato: NotificacionDto) => mapearNotificacion(dato),
  )
}
