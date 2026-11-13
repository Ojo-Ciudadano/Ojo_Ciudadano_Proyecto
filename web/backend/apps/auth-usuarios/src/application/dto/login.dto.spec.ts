import { validate } from 'class-validator';
import { plainToInstance } from 'class-transformer';
import { LoginDto } from './login.dto';

describe('LoginDto', () => {
  it('debe validar exitosamente cuando los campos son correctos', async () => {
    const dto = plainToInstance(LoginDto, {
      email: 'usuario@correo.com',
      password: 'passwordSeguro123',
    });

    const errors = await validate(dto);
    expect(errors.length).toBe(0);
  });

  it('debe rechazar un email con formato invalido', async () => {
    const dto = plainToInstance(LoginDto, {
      email: 'no-es-un-correo',
      password: 'passwordSeguro123',
    });

    const errors = await validate(dto);
    expect(errors.length).toBeGreaterThan(0);
    const emailError = errors.find((err) => err.property === 'email');
    expect(emailError).toBeDefined();
  });

  it('debe rechazar una contrasena con menos de 6 caracteres', async () => {
    const dto = plainToInstance(LoginDto, {
      email: 'usuario@correo.com',
      password: '123',
    });

    const errors = await validate(dto);
    expect(errors.length).toBeGreaterThan(0);
    const passwordError = errors.find((err) => err.property === 'password');
    expect(passwordError).toBeDefined();
  });
});
