import { ApiProperty } from '@nestjs/swagger';

export class AsignacionResponseDto {
  @ApiProperty({
    description: 'Identificador único del registro de asignación',
    format: 'uuid',
    example: '77eebc99-9c0b-4ef8-bb6d-6bb9bd380a55',
  })
  id: string;

  @ApiProperty({
    description: 'UUID del incidente asignado',
    format: 'uuid',
    example: 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
  })
  incidenteId: string;

  @ApiProperty({
    description: 'UUID del usuario administrador asignado como responsable',
    format: 'uuid',
    example: 'f1eebc99-9c0b-4ef8-bb6d-6bb9bd380a99',
  })
  usuarioAsignadoId: string;

  @ApiProperty({
    description: 'UUID del administrador que realizó la asignación',
    format: 'uuid',
    example: 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
  })
  asignadoPorId: string;

  @ApiProperty({
    description: 'Fecha y hora en que se efectuó la asignación',
    example: '2026-10-29T14:20:00.000Z',
  })
  fechaAsignacion: Date;

  @ApiProperty({
    description: 'Indica si esta asignación es la vigente actualmente para el incidente',
    example: true,
  })
  activa: boolean;
}
