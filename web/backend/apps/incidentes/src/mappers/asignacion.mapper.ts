import { AsignacionResponseDto } from '../dto/asignacion-response.dto';

export function toAsignacionResponse(asignacion: any): AsignacionResponseDto {
  return {
    id: asignacion.id,
    incidenteId: asignacion.incidenteId,
    usuarioAsignadoId: asignacion.usuarioAsignadoId,
    asignadoPorId: asignacion.asignadoPorId,
    fechaAsignacion: asignacion.fechaAsignacion,
    activa: asignacion.activa,
  };
}