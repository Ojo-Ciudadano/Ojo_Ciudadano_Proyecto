import { ApiProperty } from '@nestjs/swagger';
import { IsEnum, IsNotEmpty } from 'class-validator';
import { Role } from '../../auth/decorators/roles.decorator';

export class UpdateUsuarioRolDto {
  @ApiProperty({
    description: 'Nuevo rol que se asignará al usuario',
    enum: Role,
    example: Role.ADMIN,
  })
  @IsNotEmpty({ message: 'El rol es obligatorio.' })
  @IsEnum(Role, {
    message: 'El rol debe ser ADMIN o CIUDADANO.',
  })
  rol!: Role;
}

export class UpdateUsuarioRolResponseDto {
  @ApiProperty({
    description: 'UUID del usuario actualizado',
    format: 'uuid',
    example: 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
  })
  id!: string;

  @ApiProperty({
    description: 'Lista actualizada con el nuevo rol vigente del usuario',
    enum: Role,
    isArray: true,
    example: [Role.ADMIN],
  })
  roles!: Role[];
}
