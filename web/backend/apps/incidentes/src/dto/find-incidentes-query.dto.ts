import { Transform } from 'class-transformer';
import { IsNotEmpty, IsOptional, IsString, IsUUID } from 'class-validator';

export class FindIncidentesQueryDto {
  @IsOptional()
  @IsUUID('all', { message: 'El usuarioReportaId debe ser un UUID valido' })
  usuarioReportaId?: string;

  @IsOptional()
  @IsUUID('all', { message: 'El estadoId debe ser un UUID valido' })
  estadoId?: string;

  @IsOptional()
  @IsUUID('all', { message: 'El categoriaId debe ser un UUID valido' })
  categoriaId?: string;

  @IsOptional()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  @IsString({ message: 'La comuna debe ser un texto' })
  @IsNotEmpty({ message: 'La comuna no puede estar vacia' })
  comuna?: string;
}