import { ApiProperty } from '@nestjs/swagger';
import { IsEmail, IsString, MinLength } from 'class-validator';

export class LoginDto {
  @ApiProperty({
    description: 'Correo electrónico registrado del usuario',
    example: 'ciudadano.ejemplo@ojociudadano.cl',
  })
  @IsEmail()
  email: string;

  @ApiProperty({
    description: 'Contraseña del usuario',
    minLength: 6,
    example: 'ClaveFicticiaSegura123!',
  })
  @IsString()
  @MinLength(4)
  password: string;
}
