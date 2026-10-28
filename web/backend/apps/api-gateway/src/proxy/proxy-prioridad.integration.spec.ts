import { INestApplication, ValidationPipe } from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import { JwtService } from '@nestjs/jwt';
import { Test, TestingModule } from '@nestjs/testing';
import type { Request, Response } from 'express';
import request from 'supertest';
import { App } from 'supertest/types';

import { HttpExceptionFilter } from '../filters/http-exception.filter';
import { JwtAuthGuard } from './jwt-auth.guard';
import { ProxyController } from './proxy.controller';
import { ProxyService } from './proxy.service';
import { Role, ROLES_KEY } from './roles.decorator';
import { RolesGuard } from './roles.guard';

const JWT_SECRET = 'test-jwt-secret';
const INCIDENTE_ID = 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13';
const URL_PRIORIDAD = `/incidentes/${INCIDENTE_ID}/prioridad`;

describe('ProxyController - prioridad (integracion)', () => {
  let app: INestApplication<App>;
  let forward: jest.Mock;
  let tokenAdmin: string;
  let tokenCiudadano: string;

  beforeEach(async () => {
    const jwt = new JwtService({ secret: JWT_SECRET });
    tokenAdmin = jwt.sign({ sub: 'admin-1', roles: ['ADMIN'] });
    tokenCiudadano = jwt.sign({ sub: 'ciudadano-1', roles: ['CIUDADANO'] });

    forward = jest.fn((req: Request, res: Response) => {
      res.status(200).json({ ruta: req.path, metodo: req.method, body: req.body });
    });

    const moduleFixture: TestingModule = await Test.createTestingModule({
      controllers: [ProxyController],
      providers: [
        { provide: ProxyService, useValue: { forward } },
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
});
