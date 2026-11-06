import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import {
  IsEmail,
  IsNotEmpty,
  IsOptional,
  IsString,
  MinLength,
} from 'class-validator';

export class CreateUsuarioDto {
  @ApiProperty({
    description: 'Nombre completo del usuario',
    example: 'Camila Soto Riquelme',
  })
  @IsNotEmpty({ message: 'El nombre es requerido' })
  @IsString({ message: 'El nombre debe ser una cadena de texto' })
  nombre!: string;

  @ApiProperty({
    description: 'Correo electrónico único del usuario',
    example: 'camila.soto@ojociudadano.cl',
  })
  @IsNotEmpty({ message: 'El correo electronico es requerido' })
  @IsEmail({}, { message: 'El formato de correo electronico es invalido' })
  email!: string;

  @ApiProperty({
    description: 'Contraseña de acceso (mínimo 6 caracteres)',
    minLength: 6,
    example: 'ClaveFicticia123!',
  })
  @IsNotEmpty({ message: 'La contrasena es requerida' })
  @IsString({ message: 'La contrasena debe ser una cadena de texto' })
  @MinLength(6, { message: 'La contrasena debe tener al menos 6 caracteres' })
  password!: string;

  @ApiPropertyOptional({
    description: 'Teléfono de contacto opcional',
    example: '+56987654321',
  })
  @IsOptional()
  @IsString({ message: 'El telefono debe ser una cadena de texto' })
  telefono?: string;
}
