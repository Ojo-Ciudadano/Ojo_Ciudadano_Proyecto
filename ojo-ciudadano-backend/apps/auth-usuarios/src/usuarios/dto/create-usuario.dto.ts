import { ApiProperty } from '@nestjs/swagger';

export class CreateUsuarioDto {
  @ApiProperty({ 
    example: 'juan.perez@email.com', 
    description: 'Correo electrónico del ciudadano' 
  })
  email!: string;

  @ApiProperty({ 
    example: 'password123', 
    description: 'Contraseña en texto plano (será hasheada por el servicio)' 
  })
  password!: string;
}