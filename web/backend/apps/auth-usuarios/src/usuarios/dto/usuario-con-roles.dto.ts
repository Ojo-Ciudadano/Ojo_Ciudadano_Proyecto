import { ApiProperty } from '@nestjs/swagger';
import { Role } from '../../auth/decorators/roles.decorator';

export class UsuarioConRolesDto {
  @ApiProperty({
    description: 'UUID del usuario',
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
    description: 'Teléfono del usuario',
    required: false,
    nullable: true,
    example: '+56987654321',
  })
  telefono?: string | null;

  @ApiProperty({
    description: 'Roles vigentes asignados al usuario',
    type: [String],
    enum: Role,
    example: [Role.ADMIN],
  })
  roles: string[];
}
