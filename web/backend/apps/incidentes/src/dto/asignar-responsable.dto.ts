import { ApiProperty } from '@nestjs/swagger';
import { IsNotEmpty, IsUUID } from 'class-validator';

export class AsignarResponsableDto {
  @ApiProperty({
    description:
      'UUID del usuario responsable que será asignado al incidente (debe poseer rol ADMIN vigente)',
    format: 'uuid',
    example: 'f1eebc99-9c0b-4ef8-bb6d-6bb9bd380a99',
  })
  @IsUUID()
  @IsNotEmpty()
  usuarioAsignadoId: string;
}
