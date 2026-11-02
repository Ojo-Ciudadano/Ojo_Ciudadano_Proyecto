import { IsNotEmpty, IsUUID } from 'class-validator';

export class AsignarResponsableDto {
  @IsUUID()
  @IsNotEmpty()
  usuarioAsignadoId: string;
}