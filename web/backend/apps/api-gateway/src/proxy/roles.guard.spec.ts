
import {
  ExecutionContext,
  ForbiddenException,
  UnauthorizedException,
} from '@nestjs/common';
import { Reflector } from '@nestjs/core';

import { Role } from './roles.decorator';
import { RolesGuard } from './roles.guard';

describe('Gateway RolesGuard', () => {
  let guard: RolesGuard;
  let reflector: { getAllAndOverride: jest.Mock };

  const crearContexto = (user?: object) =>
    ({
      getHandler: () => () => undefined,
      getClass: () => class {},
      switchToHttp: () => ({
        getRequest: () => ({ user }),
      }),
    }) as ExecutionContext;

  beforeEach(() => {
    reflector = { getAllAndOverride: jest.fn().mockReturnValue(undefined) };
    guard = new RolesGuard(reflector as unknown as Reflector);
  });

  it('devuelve 401 si no existe usuario autenticado', () => {
    expect(() => guard.canActivate(crearContexto())).toThrow(
      UnauthorizedException,
    );
  });

  it('devuelve 403 si el usuario no tiene un rol permitido', () => {
    expect(() =>
      guard.canActivate(
        crearContexto({ sub: 'usuario-1', roles: ['INVITADO'] }),
      ),
    ).toThrow(ForbiddenException);
  });

  it('permite acceso al rol CIUDADANO', () => {
    expect(
      guard.canActivate(
        crearContexto({ sub: 'usuario-1', roles: ['CIUDADANO'] }),
      ),
    ).toBe(true);
  });

  it('permite acceso al rol ADMIN', () => {
    expect(
      guard.canActivate(
        crearContexto({ sub: 'admin-1', roles: ['ADMIN'] }),
      ),
    ).toBe(true);
  });

  it('devuelve 403 a un Ciudadano cuando la ruta exige ADMIN', () => {
    reflector.getAllAndOverride.mockReturnValue([Role.ADMIN]);

    expect(() =>
      guard.canActivate(
        crearContexto({ sub: 'usuario-1', roles: ['CIUDADANO'] }),
      ),
    ).toThrow(ForbiddenException);
  });

  it('permite al Administrador cuando la ruta exige ADMIN', () => {
    reflector.getAllAndOverride.mockReturnValue([Role.ADMIN]);

    expect(
      guard.canActivate(
        crearContexto({ sub: 'admin-1', roles: ['ADMIN'] }),
      ),
    ).toBe(true);
  });
});
