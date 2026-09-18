import { IsNotEmpty, IsNumber, IsOptional, IsString, IsUUID, Max, Min } from 'class-validator';
import { Type } from 'class-transformer';

export class CreateIncidenteDto {
  @IsNotEmpty({ message: 'El titulo es obligatorio' })
  @IsString({ message: 'El titulo debe ser una cadena de texto' })
  titulo!: string;

  @IsNotEmpty({ message: 'La descripcion es obligatoria' })
  @IsString({ message: 'La descripcion debe ser una cadena de texto' })
  descripcion!: string;

  @IsNotEmpty({ message: 'La categoria es obligatoria' })
  @IsUUID('all', { message: 'El id de categoria debe ser un UUID valido' })
  categoriaId!: string;

  @IsNotEmpty({ message: 'Las coordenadas de latitud son requeridas' })
  @Type(() => Number)
  @IsNumber({}, { message: 'La latitud debe ser un valor numerico' })
  @Min(-90, { message: 'La latitud minima es -90' })
  @Max(90, { message: 'La latitud maxima es 90' })
  latitud!: number;

  @IsNotEmpty({ message: 'Las coordenadas de longitud son requeridas' })
  @Type(() => Number)
  @IsNumber({}, { message: 'La longitud debe ser un valor numerico' })
  @Min(-180, { message: 'La longitud minima es -180' })
  @Max(180, { message: 'La longitud maxima es 180' })
  longitud!: number;

  @IsOptional()
  @IsString({ message: 'La direccion de referencia debe ser una cadena de texto' })
  direccionReferencia?: string;
}
