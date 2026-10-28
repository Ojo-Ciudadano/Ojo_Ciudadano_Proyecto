import { ApiProperty } from '@nestjs/swagger';
import { Role } from '../../auth/decorators/roles.decorator';

export class PerfilUsuarioDto {
  @ApiProperty({ description: 'Identificador único del usuario' })
  id: string;

  @ApiProperty({ description: 'Nombre del usuario' })
  nombre: string;

  @ApiProperty({ description: 'Correo electrónico del usuario' })
  email: string;

  @ApiProperty({
    description: 'Teléfono del usuario',
    nullable: true,
  })
  telefono: string | null;

  @ApiProperty({ description: 'Fecha de registro del usuario' })
  fechaRegistro: Date;

  @ApiProperty({
    description: 'Roles vigentes del usuario',
    enum: Role,
    isArray: true,
  })
  roles: Role[];
}