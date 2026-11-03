import { ApiProperty } from '@nestjs/swagger';
import { IsIn, IsNotEmpty, IsString } from 'class-validator';

import { ESTADOS_INCIDENTE } from '../estado/transiciones-incidente.constants';
import type { EstadoIncidente } from '../estado/transiciones-incidente.constants';

export class UpdateEstadoDto {
  @ApiProperty({
    description:
      'Nombre canónico del estado destino según la matriz de transiciones (#116)',
    enum: ESTADOS_INCIDENTE,
    example: 'En revisión',
  })
  @IsNotEmpty({ message: 'El estado es obligatorio' })
  @IsString({ message: 'El estado debe ser un texto' })
  @IsIn([...ESTADOS_INCIDENTE], {
    message: `El estado debe ser uno de: ${ESTADOS_INCIDENTE.join(', ')}`,
  })
  estado!: EstadoIncidente;

  @ApiProperty({
    description:
      'Observación obligatoria que se registra en el historial del incidente',
    example: 'Se inicia la evaluación técnica del reporte en terreno',
  })
  @IsNotEmpty({ message: 'La observacion es obligatoria' })
  @IsString({ message: 'La observacion debe ser un texto' })
  observacion!: string;
}
