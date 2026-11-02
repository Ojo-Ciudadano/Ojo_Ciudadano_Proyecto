import { IsIn, IsNotEmpty, IsString } from 'class-validator';

import { ESTADOS_INCIDENTE } from '../estado/transiciones-incidente.constants';
import type { EstadoIncidente } from '../estado/transiciones-incidente.constants';

export class UpdateEstadoDto {
  @IsNotEmpty({ message: 'El estado es obligatorio' })
  @IsString({ message: 'El estado debe ser un texto' })
  @IsIn([...ESTADOS_INCIDENTE], {
    message: `El estado debe ser uno de: ${ESTADOS_INCIDENTE.join(', ')}`,
  })
  estado!: EstadoIncidente;

  @IsNotEmpty({ message: 'La observacion es obligatoria' })
  @IsString({ message: 'La observacion debe ser un texto' })
  observacion!: string;
}
