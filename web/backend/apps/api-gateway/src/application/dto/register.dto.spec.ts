import 'reflect-metadata';
import { plainToInstance } from 'class-transformer';
import { validate } from 'class-validator';
import { RegisterDto } from './register.dto';

describe('RegisterDto (#315)', () => {
  const payloadValido = {
    nombre: 'Camila Soto Riquelme',
    email: 'camila.soto@ojociudadano.cl',
    password: 'ClaveFicticia123!',
    telefono: '+56987654321',
  };

  const validar = (data: Record<string, unknown>) => {
    const dto = plainToInstance(RegisterDto, data);
    return { dto, errorsPromise: validate(dto) };
  };

  it('acepta datos validos con telefono', async () => {
    const { dto, errorsPromise } = validar(payloadValido);
    const errors = await errorsPromise;

    expect(errors).toHaveLength(0);
    expect(dto.nombre).toBe('Camila Soto Riquelme');
    expect(dto.email).toBe('camila.soto@ojociudadano.cl');
    expect(dto.telefono).toBe('+56987654321');
  });

  it('acepta una solicitud valida omitiendo telefono o enviandolo como null', async () => {
    const { telefono: _omitido, ...sinTelefono } = payloadValido;
    expect(await validar(sinTelefono).errorsPromise).toHaveLength(0);
    expect(
      await validar({ ...sinTelefono, telefono: null }).errorsPromise,
    ).toHaveLength(0);
  });

  it('recorta espacios de nombre, email y telefono, y normaliza email a minusculas', async () => {
    const { dto, errorsPromise } = validar({
      nombre: '  Camila Soto  ',
      email: '  Camila.Soto@OjoCiudadano.CL  ',
      password: 'ClaveFicticia123!',
      telefono: '  +56 9 8765-4321  ',
    });
    const errors = await errorsPromise;

    expect(errors).toHaveLength(0);
    expect(dto.nombre).toBe('Camila Soto');
    expect(dto.email).toBe('camila.soto@ojociudadano.cl');
    expect(dto.telefono).toBe('+56 9 8765-4321');
  });

  it.each(['', '   '])('rechaza nombre vacio o solo espacios (%p)', async (nombre) => {
    const errors = await validar({ ...payloadValido, nombre }).errorsPromise;
    expect(errors.some((e) => e.property === 'nombre')).toBe(true);
  });

  it.each(['', '   ', 'correo-invalido', 'sin-arroba.cl'])(
    'rechaza correo invalido (%p)',
    async (email) => {
      const errors = await validar({ ...payloadValido, email }).errorsPromise;
      expect(errors.some((e) => e.property === 'email')).toBe(true);
    },
  );

  it.each(['', '12345'])(
    'rechaza contrasena vacia o menor a 6 caracteres (%p)',
    async (password) => {
      const errors = await validar({ ...payloadValido, password }).errorsPromise;
      expect(errors.some((e) => e.property === 'password')).toBe(true);
    },
  );

  it.each(['', '   ', 'abc', '12345', '+56 9 letras', 987654321])(
    'rechaza telefono invalido cuando se informa (%p)',
    async (telefono) => {
      const errors = await validar({ ...payloadValido, telefono }).errorsPromise;
      expect(errors.some((e) => e.property === 'telefono')).toBe(true);
    },
  );

  it.each(['+56987654321', '987654321', '(45) 2123456', '+56 9 8765-4321'])(
    'acepta formatos validos de telefono (%p)',
    async (telefono) => {
      const errors = await validar({ ...payloadValido, telefono }).errorsPromise;
      expect(errors).toHaveLength(0);
    },
  );
});
