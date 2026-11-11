import { validate } from 'class-validator';
import { plainToInstance } from 'class-transformer';
import { CreateUsuarioDto } from './create-usuario.dto';

describe('CreateUsuarioDto', () => {
  it('debe validar exitosamente con todos los campos correctos', async () => {
    const dto = plainToInstance(CreateUsuarioDto, {
      nombre: 'Juan Perez',
      email: 'juan.perez@correo.com',
      password: 'passwordSeguro123',
      telefono: '+56912345678',
    });

    const errors = await validate(dto);
    expect(errors.length).toBe(0);
  });

  it('debe validar exitosamente sin el campo opcional telefono', async () => {
    const dto = plainToInstance(CreateUsuarioDto, {
      nombre: 'Juan Perez',
      email: 'juan.perez@correo.com',
      password: 'passwordSeguro123',
    });

    const errors = await validate(dto);
    expect(errors.length).toBe(0);
  });

  it('debe fallar si el nombre esta vacio', async () => {
    const dto = plainToInstance(CreateUsuarioDto, {
      nombre: '',
      email: 'juan.perez@correo.com',
      password: 'passwordSeguro123',
    });

    const errors = await validate(dto);
    expect(errors.length).toBeGreaterThan(0);
    const nombreError = errors.find((err) => err.property === 'nombre');
    expect(nombreError).toBeDefined();
  });

  it('debe fallar si el correo es invalido', async () => {
    const dto = plainToInstance(CreateUsuarioDto, {
      nombre: 'Juan Perez',
      email: 'correo-invalido',
      password: 'passwordSeguro123',
    });

    const errors = await validate(dto);
    expect(errors.length).toBeGreaterThan(0);
    const emailError = errors.find((err) => err.property === 'email');
    expect(emailError).toBeDefined();
  });
});
