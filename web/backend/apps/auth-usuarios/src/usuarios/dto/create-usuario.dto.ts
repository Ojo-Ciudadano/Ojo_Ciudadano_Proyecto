import { IsEmail, IsNotEmpty, IsOptional, IsString, MinLength } from 'class-validator';

export class CreateUsuarioDto {
  @IsNotEmpty({ message: 'El nombre es requerido' })
  @IsString({ message: 'El nombre debe ser una cadena de texto' })
  nombre!: string;

  @IsNotEmpty({ message: 'El correo electronico es requerido' })
  @IsEmail({}, { message: 'El formato de correo electronico es invalido' })
  email!: string;

  @IsNotEmpty({ message: 'La contrasena es requerida' })
  @IsString({ message: 'La contrasena debe ser una cadena de texto' })
  @MinLength(6, { message: 'La contrasena debe tener al menos 6 caracteres' })
  password!: string;

  @IsOptional()
  @IsString({ message: 'El telefono debe ser una cadena de texto' })
  telefono?: string;
}
