import { ApiPropertyOptional } from '@nestjs/swagger';
import { Transform } from 'class-transformer';
import { IsNotEmpty, IsOptional, IsString, IsUUID } from 'class-validator';

export class FindIncidentesQueryDto {
  @ApiPropertyOptional({
    description: 'Filtrar incidentes por el UUID del usuario que los reportó',
    format: 'uuid',
    example: 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
  })
  @IsOptional()
  @IsUUID('all', { message: 'El usuarioReportaId debe ser un UUID valido' })
  usuarioReportaId?: string;

  @ApiPropertyOptional({
    description: 'Filtrar incidentes por el UUID de su estado actual',
    format: 'uuid',
    example: 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14',
  })
  @IsOptional()
  @IsUUID('all', { message: 'El estadoId debe ser un UUID valido' })
  estadoId?: string;

  @ApiPropertyOptional({
    description: 'Filtrar incidentes por el UUID de su categoría',
    format: 'uuid',
    example: 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
  })
  @IsOptional()
  @IsUUID('all', { message: 'El categoriaId debe ser un UUID valido' })
  categoriaId?: string;

  @ApiPropertyOptional({
    description:
      'Filtrar por nombre exacto de comuna (insensible a mayúsculas/minúsculas y espacios extremos)',
    example: 'Temuco',
  })
  @IsOptional()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  @IsString({ message: 'La comuna debe ser un texto' })
  @IsNotEmpty({ message: 'La comuna no puede estar vacia' })
  comuna?: string;
}
