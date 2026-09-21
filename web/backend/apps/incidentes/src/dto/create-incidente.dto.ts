import { Type } from 'class-transformer';
import { IsNotEmpty, IsNumber, IsString, IsUUID, Max, Min, ValidateNested } from 'class-validator';

export class CreateUbicacionDto {
  @Type(() => Number)
  @IsNumber({}, { message: 'La latitud debe ser un valor numerico' })
  @Min(-90, { message: 'La latitud minima es -90' })
  @Max(90, { message: 'La latitud maxima es 90' })
  latitud!: number;

  @Type(() => Number)
  @IsNumber({}, { message: 'La longitud debe ser un valor numerico' })
  @Min(-180, { message: 'La longitud minima es -180' })
  @Max(180, { message: 'La longitud maxima es 180' })
  longitud!: number;

  @IsNotEmpty({ message: 'La direccion de referencia es obligatoria' })
  @IsString()
  direccionReferencia!: string;

  @IsNotEmpty({ message: 'La comuna es obligatoria' })
  @IsString()
  comuna!: string;

  @IsNotEmpty({ message: 'La region es obligatoria' })
  @IsString()
  region!: string;

  @IsNotEmpty({ message: 'El sector es obligatorio' })
  @IsString()
  sector!: string;
}

export class CreateIncidenteDto {
  @IsNotEmpty({ message: 'El titulo es obligatorio' })
  @IsString()
  titulo!: string;

  @IsNotEmpty({ message: 'La descripcion es obligatoria' })
  @IsString()
  descripcion!: string;

  @IsNotEmpty({ message: 'La categoria es obligatoria' })
  @IsUUID('all', { message: 'El id de categoria debe ser un UUID valido' })
  categoriaId!: string;

  @IsNotEmpty({ message: 'El usuario que reporta es obligatorio' })
  @IsUUID('all', { message: 'El id de usuario debe ser un UUID valido' })
  usuarioReportaId!: string;

  @ValidateNested()
  @Type(() => CreateUbicacionDto)
  ubicacion!: CreateUbicacionDto;
}