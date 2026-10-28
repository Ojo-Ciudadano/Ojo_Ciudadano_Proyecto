import { IsInt, Max, Min } from 'class-validator';
import {
  PRIORIDAD_MAXIMA,
  PRIORIDAD_MINIMA,
} from '../prioridad/prioridad.constants';

export class UpdatePrioridadDto {
  @IsInt({ message: 'El nivel de prioridad debe ser un numero entero' })
  @Min(PRIORIDAD_MINIMA, {
    message: `El nivel de prioridad minimo es ${PRIORIDAD_MINIMA}`,
  })
  @Max(PRIORIDAD_MAXIMA, {
    message: `El nivel de prioridad maximo es ${PRIORIDAD_MAXIMA}`,
  })
  nivelPrioridad!: number;
}
