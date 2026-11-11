import { ApiProperty } from '@nestjs/swagger';
import { IsEmail, IsNotEmpty, IsString, MinLength } from 'class-validator';

export class LoginDto {
  @ApiProperty({
    description: 'Correo electrónico registrado del usuario',
    example: 'ciudadano.ejemplo@ojociudadano.cl',
  })
  @IsNotEmpty({ message: 'El correo electronico es requerido' })
  @IsEmail({}, { message: 'El formato de correo electronico es invalido' })
  email!: string;

  @ApiProperty({
    description: 'Contraseña de la cuenta (mínimo 6 caracteres)',
    minLength: 6,
    example: 'ClaveFicticiaSegura123!',
  })
  @IsNotEmpty({ message: 'La contrasena es requerida' })
  @IsString({ message: 'La contrasena debe ser una cadena de texto' })
  @MinLength(6, { message: 'La contrasena debe tener al menos 6 caracteres' })
  password!: string;
}
