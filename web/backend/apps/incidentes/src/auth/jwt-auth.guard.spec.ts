
import { ExecutionContext, ForbiddenException, UnauthorizedException } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { JwtAuthGuard } from './jwt-auth.guard';

describe('JwtAuthGuard', () => {
  let guard: JwtAuthGuard;
  let jwtService: { verifyAsync: jest.Mock };

  const crearContexto = (authorization?: string) => {
    const request = {
      headers: { authorization },
    };

    return {
      switchToHttp: () => ({
        getRequest: () => request,
      }),
    } as ExecutionContext;
  };

  beforeEach(() => {
    jwtService = {
      verifyAsync: jest.fn(),
    };

    guard = new JwtAuthGuard(jwtService as unknown as JwtService);
  });

  it('rechaza solicitudes sin token con 401', async () => {
    await expect(
      guard.canActivate(crearContexto()),
    ).rejects.toThrow(UnauthorizedException);
  });

  it('rechaza tokens inválidos con 401', async () => {
    jwtService.verifyAsync.mockRejectedValue(new Error('Token inválido'));

    await expect(
      guard.canActivate(crearContexto('Bearer token-invalido')),
    ).rejects.toThrow(UnauthorizedException);
  });

  it('rechaza usuarios sin rol autorizado con 403', async () => {
    jwtService.verifyAsync.mockResolvedValue({
      sub: 'usuario-1',
      roles: ['INVITADO'],
    });

    await expect(
      guard.canActivate(crearContexto('Bearer token-valido')),
    ).rejects.toThrow(ForbiddenException);
  });

  it('permite acceso al rol CIUDADANO', async () => {
    jwtService.verifyAsync.mockResolvedValue({
      sub: 'usuario-1',
      roles: ['CIUDADANO'],
    });

    await expect(
      guard.canActivate(crearContexto('Bearer token-valido')),
    ).resolves.toBe(true);
  });

  it('permite acceso al rol ADMIN', async () => {
    jwtService.verifyAsync.mockResolvedValue({
      sub: 'admin-1',
      roles: ['ADMIN'],
    });

    await expect(
      guard.canActivate(crearContexto('Bearer token-valido')),
    ).resolves.toBe(true);
  });
});
