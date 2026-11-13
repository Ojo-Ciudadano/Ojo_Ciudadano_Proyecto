import {
  ExecutionContext,
  ForbiddenException,
  ServiceUnavailableException,
  UnauthorizedException,
} from '@nestjs/common';
import { Reflector } from '@nestjs/core';

import { Role } from './roles.decorator';
import { RolesGuard } from './roles.guard';
import { ProxyService } from '../../application/services/proxy.service';

describe('Gateway RolesGuard', () => {
  let guard: RolesGuard;
  let reflector: { getAllAndOverride: jest.Mock };
  let proxyService: { getServiceUrl: jest.Mock };

  const fetchMock = jest.fn();
  const fetchOriginal = global.fetch;

  const crearContexto = (
    user?: object,
    authorization = 'Bearer token-de-prueba',
  ) =>
    ({
      getHandler: () => () => undefined,
      getClass: () => class {},
      switchToHttp: () => ({
        getRequest: () => ({
          user,
          headers: { authorization },
        }),
      }),
    }) as ExecutionContext;

  const simularPerfil = (
    id: string,
    roles: string[],
  ) => {
    fetchMock.mockResolvedValue({
      ok: true,
      status: 200,
      json: jest.fn().mockResolvedValue({
        id,
        roles,
      }),
    });
  };

  beforeEach(() => {
    jest.clearAllMocks();

    reflector = {
      getAllAndOverride: jest.fn().mockReturnValue(undefined),
    };

    proxyService = {
      getServiceUrl: jest.fn().mockReturnValue(
        'http://localhost:3001',
      ),
    };

    guard = new RolesGuard(
      reflector as unknown as Reflector,
      proxyService as unknown as ProxyService,
    );

    global.fetch = fetchMock as unknown as typeof fetch;
  });

  afterEach(() => {
    global.fetch = fetchOriginal;
  });

  it('devuelve 401 si no existe usuario autenticado', async () => {
    await expect(
      guard.canActivate(crearContexto()),
    ).rejects.toThrow(UnauthorizedException);

    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('devuelve 403 si el usuario no tiene un rol permitido', async () => {
    simularPerfil('usuario-1', ['INVITADO']);

    await expect(
      guard.canActivate(
        crearContexto({
          sub: 'usuario-1',
          roles: ['INVITADO'],
        }),
      ),
    ).rejects.toThrow(ForbiddenException);
  });

  it('permite acceso al rol CIUDADANO', async () => {
    simularPerfil('usuario-1', ['CIUDADANO']);

    await expect(
      guard.canActivate(
        crearContexto({
          sub: 'usuario-1',
          roles: ['CIUDADANO'],
        }),
      ),
    ).resolves.toBe(true);
  });

  it('permite acceso al rol ADMIN', async () => {
    simularPerfil('admin-1', ['ADMIN']);

    await expect(
      guard.canActivate(
        crearContexto({
          sub: 'admin-1',
          roles: ['ADMIN'],
        }),
      ),
    ).resolves.toBe(true);
  });

  it('devuelve 403 a un Ciudadano cuando la ruta exige ADMIN', async () => {
    reflector.getAllAndOverride.mockReturnValue([Role.ADMIN]);

    simularPerfil('usuario-1', ['CIUDADANO']);

    await expect(
      guard.canActivate(
        crearContexto({
          sub: 'usuario-1',
          roles: ['CIUDADANO'],
        }),
      ),
    ).rejects.toThrow(ForbiddenException);
  });

  it('permite al Administrador cuando la ruta exige ADMIN', async () => {
    reflector.getAllAndOverride.mockReturnValue([Role.ADMIN]);

    simularPerfil('admin-1', ['ADMIN']);

    await expect(
      guard.canActivate(
        crearContexto({
          sub: 'admin-1',
          roles: ['ADMIN'],
        }),
      ),
    ).resolves.toBe(true);
  });

  // Issue #122: impedir que un JWT antiguo conserve permisos ADMIN.
  it('rechaza a un administrador degradado aunque su JWT diga ADMIN', async () => {
    reflector.getAllAndOverride.mockReturnValue([Role.ADMIN]);

    // PostgreSQL ahora indica CIUDADANO.
    simularPerfil('admin-1', ['CIUDADANO']);

    await expect(
      guard.canActivate(
        crearContexto({
          sub: 'admin-1',
          roles: ['ADMIN'],
        }),
      ),
    ).rejects.toThrow(ForbiddenException);
  });

  it('permite a un usuario promovido aunque su JWT antiguo diga CIUDADANO', async () => {
    reflector.getAllAndOverride.mockReturnValue([Role.ADMIN]);

    simularPerfil('usuario-1', ['ADMIN']);

    await expect(
      guard.canActivate(
        crearContexto({
          sub: 'usuario-1',
          roles: ['CIUDADANO'],
        }),
      ),
    ).resolves.toBe(true);
  });

  it('rechaza la solicitud si auth-usuarios no está disponible', async () => {
    fetchMock.mockRejectedValue(
      new Error('Servicio no disponible'),
    );

    await expect(
      guard.canActivate(
        crearContexto({
          sub: 'admin-1',
          roles: ['ADMIN'],
        }),
      ),
    ).rejects.toThrow(ServiceUnavailableException);
  });

  it('devuelve 401 cuando auth-usuarios rechaza el JWT', async () => {
    fetchMock.mockResolvedValue({
      ok: false,
      status: 401,
    });

    await expect(
      guard.canActivate(
        crearContexto({
          sub: 'admin-1',
          roles: ['ADMIN'],
        }),
      ),
    ).rejects.toThrow(UnauthorizedException);
  });

  it('rechaza un perfil que pertenece a otro usuario', async () => {
    simularPerfil('otro-usuario', ['ADMIN']);

    await expect(
      guard.canActivate(
        crearContexto({
          sub: 'admin-1',
          roles: ['ADMIN'],
        }),
      ),
    ).rejects.toThrow(UnauthorizedException);
  });

  it('envía el JWT original al microservicio de autenticación', async () => {
    simularPerfil('admin-1', ['ADMIN']);

    await guard.canActivate(
      crearContexto(
        {
          sub: 'admin-1',
          roles: ['ADMIN'],
        },
        'Bearer jwt-original',
      ),
    );

    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:3001/usuarios/me',
      expect.objectContaining({
        method: 'GET',
        headers: {
          Authorization: 'Bearer jwt-original',
        },
      }),
    );
  });
});
