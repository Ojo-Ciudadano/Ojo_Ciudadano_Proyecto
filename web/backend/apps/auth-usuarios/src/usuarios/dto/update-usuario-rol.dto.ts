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