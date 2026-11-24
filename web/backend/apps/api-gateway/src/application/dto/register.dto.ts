import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { Transform } from 'class-transformer';
import {
  IsEmail,
  IsNotEmpty,
  IsOptional,
  IsString,
  Matches,
  MaxLength,
  MinLength,
} from 'class-validator';

export const TELEFONO_REGEX = /^\+?[0-9\s\-()]{7,20}$/;

export class RegisterDto {
  @ApiProperty({
    description: 'Nombre completo del ciudadano',
    minLength: 1,
    maxLength: 120,
    example: 'Camila Soto Riquelme',
  })
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  @IsString({ message: 'El nombre debe ser una cadena de texto' })
  @IsNotEmpty({ message: 'El nombre es requerido' })
  @MaxLength(120, { message: 'El nombre no puede superar los 120 caracteres' })
  nombre!: string;

  @ApiProperty({
    description: 'Correo electrónico único',
    format: 'email',
    maxLength: 254,
    example: 'camila.soto@ojociudadano.cl',
  })
  @Transform(({ value }) =>
    typeof value === 'string' ? value.trim().toLowerCase() : value,
  )
  @IsString({ message: 'El correo electronico debe ser una cadena de texto' })
  @IsNotEmpty({ message: 'El correo electronico es requerido' })
  @IsEmail({}, { message: 'El formato de correo electronico es invalido' })
  @MaxLength(254, {
    message: 'El correo electronico no puede superar los 254 caracteres',
  })
  email!: string;

  @ApiProperty({
    description: 'Contraseña de acceso (mínimo 6 caracteres)',
    minLength: 6,
    maxLength: 128,
    example: 'ClaveFicticia123!',
  })
  @IsString({ message: 'La contrasena debe ser una cadena de texto' })
  @IsNotEmpty({ message: 'La contrasena es requerida' })
  @MinLength(6, { message: 'La contrasena debe tener al menos 6 caracteres' })
  @MaxLength(128, {
    message: 'La contrasena no puede superar los 128 caracteres',
  })
  password!: string;

  @ApiPropertyOptional({
    description:
      'Teléfono de contacto opcional (si se informa, debe tener entre 7 y 20 caracteres numéricos válidos)',
    pattern: TELEFONO_REGEX.source,
    example: '+56987654321',
  })
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  @IsOptional()
  @IsString({ message: 'El telefono debe ser una cadena de texto' })
  @IsNotEmpty({ message: 'El telefono no puede estar vacio si se informa' })
  @Matches(TELEFONO_REGEX, {
    message:
      'El telefono debe tener un formato valido (7 a 20 caracteres numericos)',
  })
  telefono?: string;
}
