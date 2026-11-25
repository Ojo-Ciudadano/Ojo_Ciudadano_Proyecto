import { ApiProperty } from '@nestjs/swagger';
import { Type } from 'class-transformer';
import {
  IsDefined,
  IsNotEmpty,
  IsNumber,
  IsString,
  IsUUID,
  Max,
  Min,
  ValidateNested,
} from 'class-validator';

import { LIMITES_ARAUCANIA } from '../../domain/ubicacion/ciudades-araucania.constants';

export class CreateUbicacionDto {
  @ApiProperty({
    description:
      'Latitud geográfica en grados decimales (WGS84) dentro de La Araucanía',
    minimum: LIMITES_ARAUCANIA.latitudMinima,
    maximum: LIMITES_ARAUCANIA.latitudMaxima,
    example: -38.7359,
  })
  @IsDefined({ message: 'La latitud es obligatoria' })
  @IsNumber(
    { allowNaN: false, allowInfinity: false },
    { message: 'La latitud debe ser un valor numerico finito' },
  )
  @Min(LIMITES_ARAUCANIA.latitudMinima, {
    message: `La latitud minima es ${LIMITES_ARAUCANIA.latitudMinima}`,
  })
  @Max(LIMITES_ARAUCANIA.latitudMaxima, {
    message: `La latitud maxima es ${LIMITES_ARAUCANIA.latitudMaxima}`,
  })
  latitud!: number;

  @ApiProperty({
    description:
      'Longitud geográfica en grados decimales (WGS84) dentro de La Araucanía',
    minimum: LIMITES_ARAUCANIA.longitudMinima,
    maximum: LIMITES_ARAUCANIA.longitudMaxima,
    example: -72.5904,
  })
  @IsDefined({ message: 'La longitud es obligatoria' })
  @IsNumber(
    { allowNaN: false, allowInfinity: false },
    { message: 'La longitud debe ser un valor numerico finito' },
  )
  @Min(LIMITES_ARAUCANIA.longitudMinima, {
    message: `La longitud minima es ${LIMITES_ARAUCANIA.longitudMinima}`,
  })
  @Max(LIMITES_ARAUCANIA.longitudMaxima, {
    message: `La longitud maxima es ${LIMITES_ARAUCANIA.longitudMaxima}`,
  })
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
