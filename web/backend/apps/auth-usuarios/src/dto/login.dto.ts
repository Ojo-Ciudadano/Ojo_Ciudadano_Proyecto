import { IsEmail, IsNotEmpty, IsString, MinLength } from 'class-validator';

export class LoginDto {
  @IsNotEmpty({ message: 'El correo electronico es requerido' })
  @IsEmail({}, { message: 'El formato de correo electronico es invalido' })
  email!: string;

  @IsNotEmpty({ message: 'La contrasena es requerida' })
  @IsString({ message: 'La contrasena debe ser una cadena de texto' })
  @MinLength(6, { message: 'La contrasena debe tener al menos 6 caracteres' })
  password!: string;
}
