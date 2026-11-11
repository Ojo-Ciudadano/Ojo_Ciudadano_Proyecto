import 'reflect-metadata';
import { plainToInstance } from 'class-transformer';
import { validate } from 'class-validator';

import { UpdatePrioridadDto } from './update-prioridad.dto';

const validar = (payload: Record<string, unknown>) =>
  validate(plainToInstance(UpdatePrioridadDto, payload), {
    whitelist: true,
    forbidNonWhitelisted: true,
  });

describe('UpdatePrioridadDto', () => {
  it('acepta todos los niveles de la escala de prioridad (1 a 5)', async () => {
    for (const nivelPrioridad of [1, 2, 3, 4, 5]) {
      await expect(validar({ nivelPrioridad })).resolves.toHaveLength(0);
    }
  });

  it.each([
    ['cero', 0],
    ['negativo', -1],
    ['sobre el maximo', 6],
    ['decimal', 2.5],
    ['texto', 'alta'],
    ['nulo', null],
    ['ausente', undefined],
  ])('rechaza un nivel de prioridad %s', async (_caso, nivelPrioridad) => {
    const errores = await validar({ nivelPrioridad });

    expect(errores.some((error) => error.property === 'nivelPrioridad')).toBe(true);
  });

  it('rechaza campos adicionales: solo se permite nivelPrioridad', async () => {
    const errores = await validar({
      nivelPrioridad: 3,
      categoriaId: 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    });

    expect(errores.some((error) => error.property === 'categoriaId')).toBe(true);
  });
});
