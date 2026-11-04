
/**
 * Issue #144 - Tipos de notificaciones Web
 * Contrato de la API #114.
 */

export interface NotificacionDto {
  id: string
  usuarioId: string
  incidenteId: string
  mensaje: string
  leido: boolean
  fecha: string
}

export interface Notificacion {
  id: string
  usuarioId: string
  incidenteId: string
  mensaje: string
  leido: boolean
  fecha: string
}
