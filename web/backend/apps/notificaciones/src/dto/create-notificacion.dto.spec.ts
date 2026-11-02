import { validate } from 'class-validator';
import { plainToInstance } from 'class-transformer';
import { CreateNotificacionDto } from './create-notificacion.dto';

describe('CreateNotificacionDto', () => {
  const userUUID = 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380b22';
  const incidentUUID = 'c2eebc99-9c0b-4ef8-bb6d-6bb9bd380c33';

  it('debe validar exitosamente con campos validos', async () => {
    const dto = plainToInstance(CreateNotificacionDto, {
      usuarioId: userUUID,
      incidenteId: incidentUUID,
      mensaje: 'Su reporte ha cambiado de estado a En Revision',
    });

    const errors = await validate(dto);
    expect(errors.length).toBe(0);
  });

  it('debe fallar si usuarioId o incidenteId no son UUIDs validos', async () => {
    const dto = plainToInstance(CreateNotificacionDto, {
      usuarioId: 'invalido',
      incidenteId: 'invalido',
      mensaje: 'Mensaje de prueba',
    });

    const errors = await validate(dto);
    expect(errors.length).toBe(2);
  });

  it('debe fallar si el mensaje esta vacio', async () => {
    const dto = plainToInstance(CreateNotificacionDto, {
      usuarioId: userUUID,
      incidenteId: incidentUUID,
      mensaje: '',
    });

    const errors = await validate(dto);
    expect(errors.length).toBeGreaterThan(0);
    const msgError = errors.find((err) => err.property === 'mensaje');
    expect(msgError).toBeDefined();
  });

  it('debe fallar si el mensaje supera los 500 caracteres', async () => {
    const dto = plainToInstance(CreateNotificacionDto, {
      usuarioId: userUUID,
      incidenteId: incidentUUID,
      mensaje: 'a'.repeat(501),
    });

    const errors = await validate(dto);
    expect(errors.length).toBeGreaterThan(0);
    const msgError = errors.find((err) => err.property === 'mensaje');
    expect(msgError).toBeDefined();
  });
});
