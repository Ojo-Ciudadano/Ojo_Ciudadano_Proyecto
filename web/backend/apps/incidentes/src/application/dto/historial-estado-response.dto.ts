import { ApiProperty } from '@nestjs/swagger';

export class EstadoHistorialResponseDto {
  @ApiProperty({
    description: 'UUID del estado en base de datos',
    format: 'uuid',
    example: 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a15',
  })
  id!: string;

  @ApiProperty({
    description: 'Nombre canónico del estado',
    example: 'En revisión',
  })
  nombre!: string;

  @ApiProperty({
    description: 'Orden secuencial del estado en el ciclo de vida (1 a 5)',
    example: 2,
  })
  orden!: number;
}

export class HistorialEstadoResponseDto {
  @ApiProperty({
    description: 'UUID del registro de historial',
    format: 'uuid',
    example: 'e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a02',
  })
  id!: string;

  @ApiProperty({
    description: 'UUID del incidente asociado',
    format: 'uuid',
    example: 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
  })
  incidenteId!: string;

  @ApiProperty({
    description: 'Estado resultante del movimiento registrado',
    type: () => EstadoHistorialResponseDto,
  })
  estado!: EstadoHistorialResponseDto;

  @ApiProperty({
    description:
      'UUID del actor que ejecutó el cambio, tomado del registro persistido',
    format: 'uuid',
    example: 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
  })
  usuarioId!: string;

  @ApiProperty({
    description: 'Observación asociada al cambio (null cuando no existe observación)',
    type: String,
    nullable: true,
    example: 'Se inicia la evaluación del reporte',
  })
  observacion!: string | null;

  @ApiProperty({
    description: 'Fecha y hora del registro en formato ISO 8601',
    example: '2026-10-29T10:00:00.000Z',
  })
  fecha!: Date;
}
