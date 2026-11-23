import 'reflect-metadata';
import { validate, type ValidationError } from 'class-validator';
import { plainToInstance } from 'class-transformer';
import { CreateIncidenteDto } from './create-incidente.dto';

describe('CreateIncidenteDto', () => {
  const validUUID = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11';

  const ubicacionValida = {
    latitud: -38.7359,
    longitud: -72.5904,
    direccionReferencia: 'Av. Alemania 0211',
    comuna: 'Temuco',
    region: 'Araucanía',
    sector: 'Centro',
  };

  const erroresDeUbicacion = (
    errores: ValidationError[],
  ): ValidationError[] => {
    const ubicacion = errores.find((error) => error.property === 'ubicacion');
    return ubicacion?.children ?? [];
  };

  const validarConUbicacion = async (
    ubicacion: Record<string, unknown>,
  ): Promise<ValidationError[]> => {
    const dto = plainToInstance(CreateIncidenteDto, {
      titulo: 'Bache en la via publica',
      descripcion: 'Bache profundo frente al paradero de buses',
      categoriaId: validUUID,
      usuarioReportaId: validUUID,
      ubicacion,
    });

    return validate(dto);
  };

  it('debe validar exitosamente con datos correctos', async () => {
    const dto = plainToInstance(CreateIncidenteDto, {
      titulo: 'Bache en la via publica',
      descripcion: 'Bache profundo frente al paradero de buses',
      categoriaId: validUUID,
      usuarioReportaId: validUUID,
      ubicacion: ubicacionValida,
    });

    const errors = await validate(dto);
    expect(errors.length).toBe(0);
  });

  it('debe fallar si la categoriaId no es un UUID valido', async () => {
    const dto = plainToInstance(CreateIncidenteDto, {
      titulo: 'Bache en la via',
      descripcion: 'Descripcion valida',
      categoriaId: 'id-invalido',
      usuarioReportaId: validUUID,
      ubicacion: ubicacionValida,
    });

    const errors = await validate(dto);
    expect(errors.length).toBeGreaterThan(0);
    const catError = errors.find((err) => err.property === 'categoriaId');
    expect(catError).toBeDefined();
  });

  it.each([
    ['latitud', { ...ubicacionValida, latitud: -95.0 }],
    ['longitud', { ...ubicacionValida, longitud: -80.0 }],
  ])(
    'debe fallar si la %s esta fuera de los limites de La Araucania',
    async (_campo, ubicacion) => {
      const errors = await validarConUbicacion(ubicacion);

      const erroresUbicacion = erroresDeUbicacion(errors);
      expect(erroresUbicacion.length).toBeGreaterThan(0);
    },
  );

  it.each(['latitud', 'longitud'])(
    'debe fallar si falta la %s de la ubicacion manual',
    async (campo) => {
      const ubicacion: Record<string, unknown> = { ...ubicacionValida };
      delete ubicacion[campo];

      const errors = await validarConUbicacion(ubicacion);
      const erroresUbicacion = erroresDeUbicacion(errors);

      expect(erroresUbicacion.some((error) => error.property === campo)).toBe(
        true,
      );
    },
  );

  it.each([
    ['texto', 'abc'],
    ['cadena vacia', ''],
    ['NaN', Number.NaN],
    ['Infinito', Number.POSITIVE_INFINITY],
  ])('debe fallar si la latitud no es numerica (%s)', async (_caso, valor) => {
    const errors = await validarConUbicacion({
      ...ubicacionValida,
      latitud: valor,
    });

    const errorLatitud = erroresDeUbicacion(errors).find(
      (error) => error.property === 'latitud',
    );
    expect(errorLatitud).toBeDefined();
    expect(errorLatitud?.constraints?.isNumber).toBeDefined();
  });

  it('debe fallar si la longitud no es numerica', async () => {
    const errors = await validarConUbicacion({
      ...ubicacionValida,
      longitud: 'abc',
    });

    const errorLongitud = erroresDeUbicacion(errors).find(
      (error) => error.property === 'longitud',
    );
    expect(errorLongitud?.constraints?.isNumber).toBeDefined();
  });

  it('acepta una ubicacion manual valida dentro de La Araucania', async () => {
    const errors = await validarConUbicacion(ubicacionValida);

    expect(errors).toEqual([]);
  });
});
