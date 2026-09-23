import { IsNotEmpty, IsString, IsUUID } from 'class-validator';

export class CreateNotificacionDto {
  @IsNotEmpty({ message: 'El id de usuario es requerido' })
  @IsUUID('all', { message: 'El id de usuario debe ser un UUID valido' })
  usuarioId!: string;

  @IsNotEmpty({ message: 'El id de incidente es requerido' })
  @IsUUID('all', { message: 'El id de incidente debe ser un UUID valido' })
  incidenteId!: string;

  @IsNotEmpty({ message: 'El mensaje de notificacion es requerido' })
  @IsString({ message: 'El mensaje debe ser una cadena de texto' })
  mensaje!: string;
}
