import 'reflect-metadata';
import { validate } from 'class-validator';
import { plainToInstance } from 'class-transformer';
import { CreateIncidenteDto } from './create-incidente.dto';

describe('CreateIncidenteDto', () => {
  const validUUID = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11';

  it('debe validar exitosamente con datos correctos', async () => {
    const dto = plainToInstance(CreateIncidenteDto, {
      titulo: 'Bache en la via publica',
      descripcion: 'Bache profundo frente al paradero de buses',
      categoriaId: validUUID,
      latitud: -38.7359,
      longitud: -72.5904,
      direccionReferencia: 'Av. Alemania 0211',
    });

    const errors = await validate(dto);
    expect(errors.length).toBe(0);
  });

  it('debe fallar si la categoriaId no es un UUID valido', async () => {
    const dto = plainToInstance(CreateIncidenteDto, {
      titulo: 'Bache en la via',
      descripcion: 'Descripcion valida',
      categoriaId: 'id-invalido',
      latitud: -38.7359,
      longitud: -72.5904,
    });

    const errors = await validate(dto);
    expect(errors.length).toBeGreaterThan(0);
    const catError = errors.find((err) => err.property === 'categoriaId');
    expect(catError).toBeDefined();
  });

  it('debe fallar si las coordenadas estan fuera de rango', async () => {
    const dto = plainToInstance(CreateIncidenteDto, {
      titulo: 'Bache en la via',
      descripcion: 'Descripcion valida',
      categoriaId: validUUID,
      latitud: 95.0, // Latitud maxima permitida es 90
      longitud: -72.5904,
    });

    const errors = await validate(dto);
    expect(errors.length).toBeGreaterThan(0);
    const latError = errors.find((err) => err.property === 'latitud');
    expect(latError).toBeDefined();
  });
});
