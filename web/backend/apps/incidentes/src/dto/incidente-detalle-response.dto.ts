import { AsignacionResponseDto } from './asignacion-response.dto';
import { EvidenciaResponseDto } from './evidencia-response.dto';
import { HistorialEstadoResponseDto } from './historial-estado-response.dto';

export class CategoriaDetalleResponseDto {
  id!: string;
  nombre!: string;
  descripcion!: string;
  prioridadBase!: number;
  esEmergenciaCritica!: boolean;
  canalOficialContacto!: string | null;
}

export class EstadoDetalleResponseDto {
  id!: string;
  nombre!: string;
  orden!: number;
}

export class UbicacionDetalleResponseDto {
  id!: string;
  latitud!: number;
  longitud!: number;
  direccionReferencia!: string;
  comuna!: string;
  region!: string;
  sector!: string;
}

export class IncidenteDetalleResponseDto {
  id!: string;
  titulo!: string;
  descripcion!: string;
  categoriaId!: string;
  estadoId!: string;
  ubicacionId!: string;
  usuarioReportaId!: string;
  nivelPrioridad!: number | null;
  fechaCreacion!: Date;
  fechaActualizacion!: Date;

  categoria!: CategoriaDetalleResponseDto;
  estado!: EstadoDetalleResponseDto;
  ubicacion!: UbicacionDetalleResponseDto;
  evidencias!: EvidenciaResponseDto[];

  historial!: HistorialEstadoResponseDto[];
  asignacionActiva!: AsignacionResponseDto | null;
}