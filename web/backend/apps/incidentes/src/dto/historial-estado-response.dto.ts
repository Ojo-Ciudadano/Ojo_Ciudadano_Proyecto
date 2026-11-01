export class EstadoHistorialResponseDto {
  id!: string;
  nombre!: string;
  orden!: number;
}

export class HistorialEstadoResponseDto {
  id!: string;
  incidenteId!: string;
  estado!: EstadoHistorialResponseDto;
  /** Identidad del actor que realizo el cambio, tomada del historial persistido. */
  usuarioId!: string;
  /** null cuando el registro persistido no tiene observacion. */
  observacion!: string | null;
  fecha!: Date;
}