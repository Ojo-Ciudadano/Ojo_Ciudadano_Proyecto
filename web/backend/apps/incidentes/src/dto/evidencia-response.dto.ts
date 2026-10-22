export class EvidenciaResponseDto {
  id!: string;
  incidenteId!: string;
  tipoMime!: string;
  tamanoArchivo!: number;
  fechaCreacion!: Date;
  /** Ruta relativa (a la base de la API) desde donde el cliente obtiene el archivo. */
  url!: string;
}