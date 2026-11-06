import { ApiProperty } from '@nestjs/swagger';
import { Role } from '../../auth/decorators/roles.decorator';

export class PerfilUsuarioDto {
  @ApiProperty({
    description: 'Identificador único del usuario',
    format: 'uuid',
    example: 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
  })
  id: string;

  @ApiProperty({
    description: 'Nombre completo del usuario',
    example: 'Camila Soto Riquelme',
  })
  nombre: string;

  @ApiProperty({
    description: 'Correo electrónico del usuario',
    example: 'camila.soto@ojociudadano.cl',
  })
  email: string;

  @ApiProperty({
    description: 'Teléfono del usuario (null si no fue proporcionado)',
    type: String,
    nullable: true,
    example: '+56987654321',
  })
  telefono: string | null;

  @ApiProperty({
    description: 'Fecha de registro de la cuenta (ISO 8601)',
    example: '2026-10-15T12:00:00.000Z',
  })
  fechaRegistro: Date;

  @ApiProperty({
    description: 'Roles vigentes del usuario en PostgreSQL',
    enum: Role,
    isArray: true,
    example: [Role.CIUDADANO],
  })
  roles: Role[];
}
