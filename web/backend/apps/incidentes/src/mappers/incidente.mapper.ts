import { EvidenciaMetadata, toEvidenciaResponse } from './evidencia.mapper';
import { EvidenciaResponseDto } from '../dto/evidencia-response.dto';

type ConEvidencias = { evidencias?: EvidenciaMetadata[] | null };

export function toIncidenteResponse<T extends object>(
  incidente: T & ConEvidencias,
): Omit<T, 'evidencias'> & { evidencias: EvidenciaResponseDto[] } {
  const { evidencias, ...rest } = incidente;
  return { ...rest, evidencias: (evidencias ?? []).map(toEvidenciaResponse) } as Omit<T, 'evidencias'> & {
    evidencias: EvidenciaResponseDto[];
  };
}