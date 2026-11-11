import { INestApplication, ValidationPipe } from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import { JwtService } from '@nestjs/jwt';
import { Test, TestingModule } from '@nestjs/testing';
import type { Request, Response } from 'express';
import request from 'supertest';
import { App } from 'supertest/types';

import { HttpExceptionFilter } from '../filters/http-exception.filter';
import { JwtAuthGuard } from '../../infrastructure/security/jwt-auth.guard';
import { ProxyController } from './proxy.controller';
import { ProxyService } from '../../application/services/proxy.service';
import { Role, ROLES_KEY } from '../../infrastructure/security/roles.decorator';
import { RolesGuard } from '../../infrastructure/security/roles.guard';

const JWT_SECRET = 'test-jwt-secret';
const INCIDENTE_ID = 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13';
const URL_PRIORIDAD = `/incidentes/${INCIDENTE_ID}/prioridad`;

describe('ProxyController - prioridad (integracion)', () => {
  let app: INestApplication<App>;
  let forward: jest.Mock;
  let tokenAdmin: string;
  let tokenCiudadano: string;

  const fetchOriginal = global.fetch;

  const fetchMock = jest.fn();

  beforeEach(async () => {
    jest.clearAllMocks();

    const jwt = new JwtService({ secret: JWT_SECRET });

    tokenAdmin = jwt.sign({
      sub: 'admin-1',
      roles: ['ADMIN'],
    });

    tokenCiudadano = jwt.sign({
      sub: 'ciudadano-1',
      roles: ['CIUDADANO'],
    });

    forward = jest.fn((req: Request, res: Response) => {
      res.status(200).json({
        ruta: req.path,
        metodo: req.method,
        body: req.body,
      });
    });

    // Simular la consulta de roles actuales a auth-usuarios.
    fetchMock.mockImplementation(async (_url: string, options: RequestInit) => {
      const authorization = new Headers(options.headers).get('Authorization');

      if (authorization === `Bearer ${tokenAdmin}`) {
        return {
          ok: true,
          status: 200,
          json: async () => ({
            id: 'admin-1',
            roles: ['ADMIN'],
          }),
        };
      }

      if (authorization === `Bearer ${tokenCiudadano}`) {
        return {
          ok: true,
          status: 200,
          json: async () => ({
            id: 'ciudadano-1',
            roles: ['CIUDADANO'],
          }),
        };
      }

      return {
        ok: false,
        status: 401,
      };
    });

    global.fetch = fetchMock as unknown as typeof fetch;

    const moduleFixture: TestingModule = await Test.createTestingModule({
      controllers: [ProxyController],
      providers: [
        {
          provide: ProxyService,
          useValue: {
            forward,
            getServiceUrl: jest.fn().mockReturnValue(
              'http://localhost:3001',
            ),
          },
        },
        JwtAuthGuard,
        RolesGuard,
        { provide: JwtService, useValue: jwt },
        { provide: Reflector, useValue: new Reflector() },
      ],
    }).compile();

    app = moduleFixture.createNestApplication();

    app.useGlobalFilters(new HttpExceptionFilter());

    app.useGlobalPipes(
      new ValidationPipe({
        whitelist: true,
        forbidNonWhitelisted: true,
        transform: true,
      }),
    );

    await app.init();
  });

  afterEach(async () => {
    await app.close();
    global.fetch = fetchOriginal;
  });

  it('responde 401 sin autenticacion', async () => {
    await request(app.getHttpServer())
      .patch(URL_PRIORIDAD)
      .send({ nivelPrioridad: 4 })
      .expect(401);

    expect(forward).not.toHaveBeenCalled();
  });

  it('responde 403 a un Ciudadano autenticado', async () => {
    await request(app.getHttpServer())
      .patch(URL_PRIORIDAD)
      .set('Authorization', `Bearer ${tokenCiudadano}`)
      .send({ nivelPrioridad: 4 })
      .expect(403);

    expect(forward).not.toHaveBeenCalled();
  });

  it('reenvia la modificacion al microservicio cuando el usuario es Administrador', async () => {
    const respuesta = await request(app.getHttpServer())
      .patch(URL_PRIORIDAD)
      .set('Authorization', `Bearer ${tokenAdmin}`)
      .send({ nivelPrioridad: 4 })
      .expect(200);

    expect(forward).toHaveBeenCalledTimes(1);

    expect(respuesta.body).toMatchObject({
      ruta: URL_PRIORIDAD,
      metodo: 'PATCH',
      body: { nivelPrioridad: 4 },
    });
  });

  it('declara explicitamente el rol ADMIN en la ruta de prioridad', () => {
    const reflector = new Reflector();

    expect(
      reflector.getAllAndOverride(ROLES_KEY, [
        ProxyController.prototype.handleIncidentesPrioridadProxy,
        ProxyController,
      ]),
    ).toEqual([Role.ADMIN]);
  });

  it('mantiene el acceso general de incidentes para un Ciudadano', async () => {
    await request(app.getHttpServer())
      .get('/incidentes')
      .set('Authorization', `Bearer ${tokenCiudadano}`)
      .expect(200);

    expect(forward).toHaveBeenCalledTimes(1);
  });

  // Issue #122: comprobar que los permisos se actualizan inmediatamente.
  it('rechaza un JWT antiguo de ADMIN cuando el usuario fue degradado', async () => {
    fetchMock.mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({
        id: 'admin-1',
        roles: ['CIUDADANO'],
      }),
    });

    await request(app.getHttpServer())
      .patch(URL_PRIORIDAD)
      .set('Authorization', `Bearer ${tokenAdmin}`)
      .send({ nivelPrioridad: 4 })
      .expect(403);

    expect(forward).not.toHaveBeenCalled();
  });

  it('deniega acceso si falla la consulta de roles actuales', async () => {
    fetchMock.mockRejectedValue(
      new Error('Servicio de autenticación no disponible'),
    );

    await request(app.getHttpServer())
      .patch(URL_PRIORIDAD)
      .set('Authorization', `Bearer ${tokenAdmin}`)
      .send({ nivelPrioridad: 4 })
      .expect(503);

    expect(forward).not.toHaveBeenCalled();
  });
});
