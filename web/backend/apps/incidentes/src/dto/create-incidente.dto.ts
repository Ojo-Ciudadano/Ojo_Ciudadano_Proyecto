import { ApiProperty } from '@nestjs/swagger';
import { Type } from 'class-transformer';
import {
  IsNotEmpty,
  IsNumber,
  IsString,
  IsUUID,
  Max,
  Min,
  ValidateNested,
} from 'class-validator';

export class CreateUbicacionDto {
  @ApiProperty({
    description: 'Latitud geográfica en grados decimales (WGS84)',
    minimum: -90,
    maximum: 90,
    example: -38.7359,
  })
  @Type(() => Number)
  @IsNumber({}, { message: 'La latitud debe ser un valor numerico' })
  @Min(-90, { message: 'La latitud minima es -90' })
  @Max(90, { message: 'La latitud maxima es 90' })
  latitud!: number;

  @ApiProperty({
    description: 'Longitud geográfica en grados decimales (WGS84)',
    minimum: -180,
    maximum: 180,
    example: -72.5904,
  })
  @Type(() => Number)
  @IsNumber({}, { message: 'La longitud debe ser un valor numerico' })
  @Min(-180, { message: 'La longitud minima es -180' })
  @Max(180, { message: 'La longitud maxima es 180' })
  longitud!: number;

  @ApiProperty({
    description: 'Dirección o punto de referencia del incidente',
    example: 'Av. Alemania 0422, frente al campus universitario',
  })
  @IsNotEmpty({ message: 'La direccion de referencia es obligatoria' })
  @IsString()
  direccionReferencia!: string;

  @ApiProperty({
    description: 'Comuna donde ocurre el incidente',
    example: 'Temuco',
  })
  @IsNotEmpty({ message: 'La comuna es obligatoria' })
  @IsString()
  comuna!: string;

  @ApiProperty({
    description: 'Región administrativa',
    example: 'La Araucanía',
  })
  @IsNotEmpty({ message: 'La region es obligatoria' })
  @IsString()
  region!: string;

  @ApiProperty({
    description: 'Sector o barrio de referencia dentro de la comuna',
    example: 'Poniente',
  })
  @IsNotEmpty({ message: 'El sector es obligatorio' })
  @IsString()
  sector!: string;
}

export class CreateIncidenteDto {
  @ApiProperty({
    description: 'Título breve y descriptivo del incidente reportado',
    example: 'Luminaria apagada en Av. Alemania',
  })
  @IsNotEmpty({ message: 'El titulo es obligatorio' })
  @IsString()
  titulo!: string;

  @ApiProperty({
    description: 'Descripción detallada de la situación observada',
    example:
      'El poste de alumbrado público ubicado en la esquina se encuentra apagado desde hace dos noches.',
  })
  @IsNotEmpty({ message: 'La descripcion es obligatoria' })
  @IsString()
  descripcion!: string;

  @ApiProperty({
    description:
      'UUID de la categoría del incidente (define automáticamente la prioridad inicial)',
    format: 'uuid',
    example: 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
  })
  @IsNotEmpty({ message: 'La categoria es obligatoria' })
  @IsUUID('all', { message: 'El id de categoria debe ser un UUID valido' })
  categoriaId!: string;

  @ApiProperty({
    description: 'UUID del usuario que reporta el incidente',
    format: 'uuid',
    example: 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
  })
  @IsNotEmpty({ message: 'El usuario que reporta es obligatorio' })
  @IsUUID('all', { message: 'El id de usuario debe ser un UUID valido' })
  usuarioReportaId!: string;

  @ApiProperty({
    description: 'Datos geográficos y de referencia de ubicación del incidente',
    type: () => CreateUbicacionDto,
  })
  @ValidateNested()
  @Type(() => CreateUbicacionDto)
  ubicacion!: CreateUbicacionDto;
}
