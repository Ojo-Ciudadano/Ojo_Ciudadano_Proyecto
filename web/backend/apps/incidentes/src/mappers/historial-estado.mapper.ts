import { HistorialEstadoResponseDto } from '../dto/historial-estado-response.dto';

type HistorialEstadoConEstado = {
  id: string;
  incidenteId: string;
  usuarioId: string;
  observacion: string | null;
  fecha: Date;
  estado: {
    id: string;
    nombre: string;
    orden: number;
  };
};

export function toHistorialEstadoResponse(
  registro: HistorialEstadoConEstado,
): HistorialEstadoResponseDto {
  return {
    id: registro.id,
    incidenteId: registro.incidenteId,
    estado: {
      id: registro.estado.id,
      nombre: registro.estado.nombre,
      orden: registro.estado.orden,
    },
    usuarioId: registro.usuarioId,
    observacion: registro.observacion ?? null,
    fecha: registro.fecha,
  };
}