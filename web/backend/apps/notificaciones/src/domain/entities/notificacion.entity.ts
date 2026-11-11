export interface NotificacionEntity {
  id: string;
  usuarioId: string;
  incidenteId: string;
  mensaje: string;
  leido: boolean;
  fecha: Date;
}
