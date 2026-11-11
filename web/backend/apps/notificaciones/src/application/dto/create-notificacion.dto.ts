import { ApiProperty } from '@nestjs/swagger';
import { IsNotEmpty, IsString, IsUUID } from 'class-validator';

export class CreateNotificacionDto {
  @ApiProperty({
    description: 'UUID del usuario destinatario de la notificación',
    format: 'uuid',
    example: 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
  })
  @IsNotEmpty({ message: 'El id de usuario es requerido' })
  @IsUUID('all', { message: 'El id de usuario debe ser un UUID valido' })
  usuarioId!: string;

  @ApiProperty({
    description: 'UUID del incidente relacionado con la notificación',
    format: 'uuid',
    example: 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
  })
  @IsNotEmpty({ message: 'El id de incidente es requerido' })
  @IsUUID('all', { message: 'El id de incidente debe ser un UUID valido' })
  incidenteId!: string;

  @ApiProperty({
    description: 'Mensaje descriptivo de la notificación',
    example:
      'El incidente "Luminaria apagada en Av. Alemania" cambió de estado "Nuevo" a "En revisión".',
  })
  @IsNotEmpty({ message: 'El mensaje de notificacion es requerido' })
  @IsString({ message: 'El mensaje debe ser una cadena de texto' })
  mensaje!: string;
}

export class NotificacionResponseDto {
  @ApiProperty({
    description: 'UUID de la notificación',
    format: 'uuid',
    example: '11eebc99-9c0b-4ef8-bb6d-6bb9bd380a77',
  })
  id!: string;

  @ApiProperty({
    description: 'UUID del usuario destinatario',
    format: 'uuid',
    example: 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
  })
  usuarioId!: string;

  @ApiProperty({
    description: 'UUID del incidente asociado',
    format: 'uuid',
    example: 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
  })
  incidenteId!: string;

  @ApiProperty({
    description: 'Contenido del mensaje de la notificación',
    example:
      'El incidente "Luminaria apagada en Av. Alemania" cambió de estado "Nuevo" a "En revisión".',
  })
  mensaje!: string;

  @ApiProperty({
    description: 'Indica si la notificación ha sido leída',
    example: false,
  })
  leido!: boolean;

  @ApiProperty({
    description: 'Fecha y hora de emisión de la notificación (ISO 8601)',
    example: '2026-10-29T10:05:00.000Z',
  })
  fecha!: Date;
}
