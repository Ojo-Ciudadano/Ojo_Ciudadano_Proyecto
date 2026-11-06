import {
  ExecutionContext,
  ForbiddenException,
  UnauthorizedException,
} from '@nestjs/common';
import { Reflector } from '@nestjs/core';

import { Role } from './roles.decorator';
import { RolesGuard } from './roles.guard';

describe('RolesGuard (incidentes)', () => {
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
    reflector = { getAllAndOverride: jest.fn() };
    guard = new RolesGuard(reflector as unknown as Reflector);
  });

  it('responde 401 si no hay usuario autenticado', () => {
    reflector.getAllAndOverride.mockReturnValue([Role.ADMIN]);

    expect(() => guard.canActivate(crearContexto())).toThrow(
      UnauthorizedException,
    );
  });

  it('permite acceso al Administrador cuando el endpoint exige ADMIN', () => {
    reflector.getAllAndOverride.mockReturnValue([Role.ADMIN]);

    expect(
      guard.canActivate(crearContexto({ sub: 'admin-1', roles: ['ADMIN'] })),
    ).toBe(true);
  });

  it('responde 403 a un Ciudadano cuando el endpoint exige ADMIN', () => {
    reflector.getAllAndOverride.mockReturnValue([Role.ADMIN]);

    expect(() =>
      guard.canActivate(
        crearContexto({ sub: 'ciudadano-1', roles: ['CIUDADANO'] }),
      ),
    ).toThrow(ForbiddenException);
  });

  it('permite cualquier rol autenticado si el endpoint no declara roles', () => {
    reflector.getAllAndOverride.mockReturnValue(undefined);

    expect(
      guard.canActivate(
        crearContexto({ sub: 'ciudadano-1', roles: ['CIUDADANO'] }),
      ),
    ).toBe(true);
  });

  it('sigue exigiendo autenticacion si el endpoint no declara roles', () => {
    reflector.getAllAndOverride.mockReturnValue(undefined);

    expect(() => guard.canActivate(crearContexto())).toThrow(
      UnauthorizedException,
    );
  });
});
