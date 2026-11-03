import { ApiProperty } from '@nestjs/swagger';
import { AsignacionResponseDto } from './asignacion-response.dto';
import { EvidenciaResponseDto } from './evidencia-response.dto';
import { HistorialEstadoResponseDto } from './historial-estado-response.dto';

export class CategoriaDetalleResponseDto {
  @ApiProperty({
    description: 'UUID de la categoría',
    format: 'uuid',
    example: 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
  })
  id!: string;

  @ApiProperty({
    description: 'Nombre de la categoría',
    example: 'Alumbrado público',
  })
  nombre!: string;

  @ApiProperty({
    description: 'Descripción de la categoría',
    example: 'Fallas o desperfectos en luminarias y tendido eléctrico público',
  })
  descripcion!: string;

  @ApiProperty({
    description: 'Prioridad inicial asignada por defecto a esta categoría (1 a 5)',
    minimum: 1,
    maximum: 5,
    example: 3,
  })
  prioridadBase!: number;

  @ApiProperty({
    description: 'Indica si la categoría corresponde a una emergencia crítica',
    example: false,
  })
  esEmergenciaCritica!: boolean;

  @ApiProperty({
    description: 'Canal oficial de contacto de emergencia (si aplica)',
    type: String,
    nullable: true,
    example: '1409 Seguridad Municipal',
  })
  canalOficialContacto!: string | null;
}

export class EstadoDetalleResponseDto {
  @ApiProperty({
    description: 'UUID del estado',
    format: 'uuid',
    example: 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14',
  })
  id!: string;

  @ApiProperty({
    description: 'Nombre canónico del estado',
    example: 'Nuevo',
  })
  nombre!: string;

  @ApiProperty({
    description: 'Orden secuencial del estado (1 a 5)',
    example: 1,
  })
  orden!: number;
}

export class UbicacionDetalleResponseDto {
  @ApiProperty({
    description: 'UUID de la ubicación',
    format: 'uuid',
    example: '99eebc99-9c0b-4ef8-bb6d-6bb9bd380a33',
  })
  id!: string;

  @ApiProperty({
    description: 'Latitud geográfica',
    example: -38.7359,
  })
  latitud!: number;

  @ApiProperty({
    description: 'Longitud geográfica',
    example: -72.5904,
  })
  longitud!: number;

  @ApiProperty({
    description: 'Dirección o punto de referencia',
    example: 'Av. Alemania 0422, frente al campus universitario',
  })
  direccionReferencia!: string;

  @ApiProperty({
    description: 'Comuna del incidente',
    example: 'Temuco',
  })
  comuna!: string;

  @ApiProperty({
    description: 'Región administrativa',
    example: 'La Araucanía',
  })
  region!: string;

  @ApiProperty({
    description: 'Sector dentro de la comuna',
    example: 'Poniente',
  })
  sector!: string;
}

export class IncidenteResumenResponseDto {
  @ApiProperty({
    description: 'UUID del incidente',
    format: 'uuid',
    example: 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
  })
  id!: string;

  @ApiProperty({
    description: 'Título del incidente',
    example: 'Luminaria apagada en Av. Alemania',
  })
  titulo!: string;

  @ApiProperty({
    description: 'Descripción detallada del incidente',
    example: 'El poste de alumbrado público ubicado en la esquina no enciende.',
  })
  descripcion!: string;

  @ApiProperty({
    description: 'UUID de la categoría asociada',
    format: 'uuid',
    example: 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
  })
  categoriaId!: string;

  @ApiProperty({
    description: 'UUID del estado actual',
    format: 'uuid',
    example: 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14',
  })
  estadoId!: string;

  @ApiProperty({
    description: 'UUID de la ubicación registrada',
    format: 'uuid',
    example: '99eebc99-9c0b-4ef8-bb6d-6bb9bd380a33',
  })
  ubicacionId!: string;

  @ApiProperty({
    description: 'UUID del usuario que reportó el incidente',
    format: 'uuid',
    example: 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
  })
  usuarioReportaId!: string;

  @ApiProperty({
    description:
      'Nivel de prioridad del incidente (inicializado desde categoria.prioridadBase, modificable por ADMIN entre 1 y 5)',
    type: Number,
    nullable: true,
    minimum: 1,
    maximum: 5,
    example: 3,
  })
  nivelPrioridad!: number | null;

  @ApiProperty({
    description: 'Fecha de creación del incidente (ISO 8601)',
    example: '2026-10-29T10:00:00.000Z',
  })
  fechaCreacion!: Date;

  @ApiProperty({
    description: 'Fecha de última actualización del incidente (ISO 8601)',
    example: '2026-10-29T10:00:00.000Z',
  })
  fechaActualizacion!: Date;

  @ApiProperty({
    description: 'Información de la categoría del incidente',
    type: () => CategoriaDetalleResponseDto,
  })
  categoria!: CategoriaDetalleResponseDto;

  @ApiProperty({
    description: 'Estado actual del incidente',
    type: () => EstadoDetalleResponseDto,
  })
  estado!: EstadoDetalleResponseDto;

  @ApiProperty({
    description: 'Ubicación geográfica del incidente',
    type: () => UbicacionDetalleResponseDto,
  })
  ubicacion!: UbicacionDetalleResponseDto;

  @ApiProperty({
    description:
      'Colección de evidencias fotográficas asociadas ([] cuando no existen evidencias, #106)',
    type: () => [EvidenciaResponseDto],
  })
  evidencias!: EvidenciaResponseDto[];
}

export class IncidenteDetalleResponseDto extends IncidenteResumenResponseDto {
  @ApiProperty({
    description:
      'Historial cronológico ascendente de cambios de estado y asignaciones ([] si no hay registros, #125)',
    type: () => [HistorialEstadoResponseDto],
  })
  historial!: HistorialEstadoResponseDto[];

  @ApiProperty({
    description:
      'Asignación de responsable vigente para el incidente (null si no posee responsable activo, #125)',
    type: () => AsignacionResponseDto,
    nullable: true,
  })
  asignacionActiva!: AsignacionResponseDto | null;
}
