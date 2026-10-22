
import {
  ExecutionContext,
  ForbiddenException,
  UnauthorizedException,
} from '@nestjs/common';
import { RolesGuard } from './roles.guard';

describe('Gateway RolesGuard', () => {
  let guard: RolesGuard;

  const crearContexto = (user?: object) =>
    ({
      switchToHttp: () => ({
        getRequest: () => ({ user }),
      }),
    }) as ExecutionContext;

  beforeEach(() => {
    guard = new RolesGuard();
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
});
