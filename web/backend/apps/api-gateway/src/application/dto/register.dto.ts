import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import {
  IsEmail,
  IsNotEmpty,
  IsOptional,
  IsString,
  MinLength,
} from 'class-validator';

export class RegisterDto {
  @ApiProperty({
    description: 'Nombre completo del ciudadano',
    example: 'Camila Soto Riquelme',
  })
  @IsNotEmpty()
  @IsString()
  nombre: string;

  @ApiProperty({
    description: 'Correo electrónico único',
    example: 'camila.soto@ojociudadano.cl',
  })
  @IsEmail()
  email: string;

  @ApiProperty({
    description: 'Contraseña de acceso (mínimo 6 caracteres)',
    minLength: 6,
    example: 'ClaveFicticia123!',
  })
  @IsString()
  @MinLength(6)
  password: string;

  @ApiPropertyOptional({
    description: 'Teléfono de contacto opcional',
    example: '+56987654321',
  })
  @IsOptional()
  @IsString()
  telefono?: string;
}
