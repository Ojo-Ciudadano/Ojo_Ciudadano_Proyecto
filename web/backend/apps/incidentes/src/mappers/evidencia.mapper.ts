import { EvidenciaResponseDto } from '../dto/evidencia-response.dto';

export interface EvidenciaMetadata {
  id: string;
  incidenteId: string;
  tipoMime: string;
  tamanoArchivo: number;
  fechaCreacion: Date;
}

export const evidenciaSelect = {
  id: true,
  incidenteId: true,
  tipoMime: true,
  tamanoArchivo: true,
  fechaCreacion: true,
} as const;

export const buildEvidenciaUrl = (evidenciaId: string): string =>
  `/evidencias/${evidenciaId}/archivo`;

export function toEvidenciaResponse(evidencia: EvidenciaMetadata): EvidenciaResponseDto {
  return {
    id: evidencia.id,
    incidenteId: evidencia.incidenteId,
    tipoMime: evidencia.tipoMime,
    tamanoArchivo: evidencia.tamanoArchivo,
    fechaCreacion: evidencia.fechaCreacion,
    url: buildEvidenciaUrl(evidencia.id),
  };
}