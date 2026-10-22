
import {
  ExecutionContext,
  ForbiddenException,
  UnauthorizedException,
} from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import { RolesGuard } from './roles.guards';
import { Role } from '../decorators/roles.decorator';

describe('RolesGuard', () => {
  let guard: RolesGuard;
  let reflector: { getAllAndOverride: jest.Mock };

  const crearContexto = (user?: object) =>
    ({
      getHandler: () => jest.fn(),
      getClass: () => class TestController {},
      switchToHttp: () => ({
        getRequest: () => ({ user }),
      }),
    }) as unknown as ExecutionContext;

  beforeEach(() => {
    reflector = {
      getAllAndOverride: jest.fn(),
    };

    guard = new RolesGuard(reflector as unknown as Reflector);
  });

  it('permite endpoints sin roles requeridos', () => {
    reflector.getAllAndOverride.mockReturnValue(undefined);

    expect(guard.canActivate(crearContexto())).toBe(true);
  });

  it('devuelve 401 si no hay usuario autenticado', () => {
    reflector.getAllAndOverride.mockReturnValue([Role.ADMIN]);

    expect(() => guard.canActivate(crearContexto())).toThrow(
      UnauthorizedException,
    );
  });

  it('devuelve 403 si CIUDADANO intenta acceder como ADMIN', () => {
    reflector.getAllAndOverride.mockReturnValue([Role.ADMIN]);

    expect(() =>
      guard.canActivate(
        crearContexto({ sub: 'usuario-1', roles: ['CIUDADANO'] }),
      ),
    ).toThrow(ForbiddenException);
  });

  it('permite acceder al usuario ADMIN', () => {
    reflector.getAllAndOverride.mockReturnValue([Role.ADMIN]);

    expect(
      guard.canActivate(
        crearContexto({ sub: 'admin-1', roles: ['ADMIN'] }),
      ),
    ).toBe(true);
  });
});
