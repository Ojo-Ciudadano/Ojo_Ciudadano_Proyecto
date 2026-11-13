import { ApiProperty } from '@nestjs/swagger';
import { IsInt, Max, Min } from 'class-validator';
import {
  PRIORIDAD_MAXIMA,
  PRIORIDAD_MINIMA,
} from '../../domain/prioridad/prioridad.constants';

export class UpdatePrioridadDto {
  @ApiProperty({
    description: 'Nuevo nivel de prioridad operativa del incidente (1 = mínima, 5 = crítica)',
    minimum: PRIORIDAD_MINIMA,
    maximum: PRIORIDAD_MAXIMA,
    example: 4,
  })
  @IsInt({ message: 'El nivel de prioridad debe ser un numero entero' })
  @Min(PRIORIDAD_MINIMA, {
    message: `El nivel de prioridad minimo es ${PRIORIDAD_MINIMA}`,
  })
  @Max(PRIORIDAD_MAXIMA, {
    message: `El nivel de prioridad maximo es ${PRIORIDAD_MAXIMA}`,
  })
  nivelPrioridad!: number;
}
