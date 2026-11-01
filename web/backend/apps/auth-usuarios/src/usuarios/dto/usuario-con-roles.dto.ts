import { ApiProperty } from '@nestjs/swagger';

export class UsuarioConRolesDto {
  @ApiProperty()
  id: string;

  @ApiProperty()
  nombre: string;

  @ApiProperty()
  email: string;

  @ApiProperty({ required: false, nullable: true })
  telefono?: string | null;

  @ApiProperty({ type: [String] })
  roles: string[];
}