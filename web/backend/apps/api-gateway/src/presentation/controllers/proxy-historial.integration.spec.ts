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
import { RolesGuard } from '../../infrastructure/security/roles.guard';

const JWT_SECRET = 'test-jwt-secret';
const INCIDENTE_ID = 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13';
const URL_HISTORIAL = `/incidentes/${INCIDENTE_ID}/historial`;

describe('ProxyController - historial (integracion)', () => {
  let app: INestApplication<App>;
  let forward: jest.Mock;
  let tokenCiudadano: string;
  let tokenSinRol: string;

  // Guardar fetch original para restaurarlo después de cada prueba
  const fetchOriginal = global.fetch;

  beforeEach(async () => {
    const jwt = new JwtService({ secret: JWT_SECRET });

    tokenCiudadano = jwt.sign({
      sub: 'ciudadano-1',
      roles: ['CIUDADANO'],
    });

    tokenSinRol = jwt.sign({
      sub: 'usuario-1',
      roles: ['OTRO'],
    });

    // Simular el reenvío de peticiones del API Gateway
    forward = jest.fn((req: Request, res: Response) => {
      res.status(200).json({
        ruta: req.path,
        metodo: req.method,
      });
    });

    // Simular la consulta al microservicio auth-usuarios
    global.fetch = jest.fn(async (_url, options) => {
      const authorization = new Headers(options?.headers).get(
        'Authorization',
      );

      const token = authorization?.replace(/^Bearer\s+/i, '');

      const payload = token ? jwt.verify(token) : null;

      return {
        ok: true,
        status: 200,
        json: async () => ({
          id: payload?.sub,
          roles:
            payload?.sub === 'ciudadano-1'
              ? ['CIUDADANO']
              : ['OTRO'],
        }),
      } as globalThis.Response;
    }) as typeof fetch;

    const moduleFixture: TestingModule =
      await Test.createTestingModule({
        controllers: [ProxyController],
        providers: [
          {
            provide: ProxyService,
            useValue: {
              forward,
              getServiceUrl: jest
                .fn()
                .mockReturnValue('http://localhost:3001'),
            },
          },
          JwtAuthGuard,
          RolesGuard,
          {
            provide: JwtService,
            useValue: jwt,
          },
          {
            provide: Reflector,
            useValue: new Reflector(),
          },
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
    // Restaurar fetch para no afectar otras pruebas
    global.fetch = fetchOriginal;

    await app.close();
  });

  it('responde 401 sin autenticacion y no reenvia', async () => {
    await request(app.getHttpServer())
      .get(URL_HISTORIAL)
      .expect(401);

    expect(forward).not.toHaveBeenCalled();
  });

  it('responde 403 si el token no tiene un rol permitido', async () => {
    await request(app.getHttpServer())
      .get(URL_HISTORIAL)
      .set('Authorization', `Bearer ${tokenSinRol}`)
      .expect(403);

    expect(forward).not.toHaveBeenCalled();
  });

  it('reenvia GET /incidentes/:id/historial al microservicio para un Ciudadano', async () => {
    const respuesta = await request(app.getHttpServer())
      .get(URL_HISTORIAL)
      .set('Authorization', `Bearer ${tokenCiudadano}`)
      .expect(200);

    expect(forward).toHaveBeenCalledTimes(1);

    expect(respuesta.body).toMatchObject({
      ruta: URL_HISTORIAL,
      metodo: 'GET',
    });
  });
});
