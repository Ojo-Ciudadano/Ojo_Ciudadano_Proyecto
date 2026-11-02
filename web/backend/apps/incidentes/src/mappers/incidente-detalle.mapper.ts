import { AsignacionResponseDto } from '../dto/asignacion-response.dto';
import { HistorialEstadoResponseDto } from '../dto/historial-estado-response.dto';
import { IncidenteDetalleResponseDto } from '../dto/incidente-detalle-response.dto';
import { EvidenciaMetadata } from './evidencia.mapper';
import { toIncidenteResponse } from './incidente.mapper';

type IncidenteConEvidencias = {
  evidencias?: EvidenciaMetadata[] | null;
};

export function toIncidenteDetalleResponse<T extends object>(
  incidente: T & IncidenteConEvidencias,
  historial: HistorialEstadoResponseDto[],
  asignacionActiva: AsignacionResponseDto | null,
): IncidenteDetalleResponseDto {
  return {
    ...toIncidenteResponse(incidente),
    historial,
    asignacionActiva,
  } as IncidenteDetalleResponseDto;
}