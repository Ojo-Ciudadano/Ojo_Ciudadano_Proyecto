import { ExecutionContext, UnauthorizedException } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { JwtAuthGuard } from './jwt-auth.guard';

describe('JwtAuthGuard - Notificaciones (#114)', () => {
  let guard: JwtAuthGuard;
  let jwtService: {
    verifyAsync: jest.Mock;
  };

  const USUARIO_ID = 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380b22';

  beforeEach(() => {
    jwtService = {
      verifyAsync: jest.fn(),
    };

    guard = new JwtAuthGuard(
      jwtService as unknown as JwtService,
    );
  });

  function crearContexto(authorization?: string) {
    const request = {
      headers: authorization
        ? { authorization }
        : {},
      user: undefined as unknown,
    };

    const context = {
      switchToHttp: () => ({
        getRequest: () => request,
      }),
    } as ExecutionContext;

    return { context, request };
  }

  it('rechaza solicitudes sin token con 401 Unauthorized', async () => {
    const { context } = crearContexto();

    await expect(
      guard.canActivate(context),
    ).rejects.toBeInstanceOf(UnauthorizedException);

    expect(jwtService.verifyAsync).not.toHaveBeenCalled();
  });

  it('rechaza solicitudes con formato de autorizacion incorrecto', async () => {
    const { context } = crearContexto('Basic abc123');

    await expect(
      guard.canActivate(context),
    ).rejects.toBeInstanceOf(UnauthorizedException);

    expect(jwtService.verifyAsync).not.toHaveBeenCalled();
  });

  it('rechaza tokens invalidos o expirados', async () => {
    const { context } = crearContexto('Bearer token-invalido');

    jwtService.verifyAsync.mockRejectedValue(
      new Error('invalid signature'),
    );

    await expect(
      guard.canActivate(context),
    ).rejects.toBeInstanceOf(UnauthorizedException);

    expect(jwtService.verifyAsync).toHaveBeenCalledWith(
      'token-invalido',
    );
  });

  it('rechaza tokens que no contienen un usuario valido', async () => {
    const { context } = crearContexto('Bearer token-sin-usuario');

    jwtService.verifyAsync.mockResolvedValue({
      email: 'usuario@ejemplo.cl',
    });

    await expect(
      guard.canActivate(context),
    ).rejects.toBeInstanceOf(UnauthorizedException);
  });

  it('permite solicitudes con JWT valido y asigna el usuario autenticado', async () => {
    const { context, request } = crearContexto('Bearer token-valido');

    const payload = {
      sub: USUARIO_ID,
      email: 'usuario@ejemplo.cl',
      roles: ['CIUDADANO'],
    };

    jwtService.verifyAsync.mockResolvedValue(payload);

    const resultado = await guard.canActivate(context);

    expect(resultado).toBe(true);

    expect(jwtService.verifyAsync).toHaveBeenCalledWith(
      'token-valido',
    );

    expect(request.user).toEqual(payload);
  });
});