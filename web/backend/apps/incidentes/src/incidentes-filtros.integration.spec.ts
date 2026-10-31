/// <reference types="jest" />
import { INestApplication, ValidationPipe } from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import { JwtService } from '@nestjs/jwt';
import { Test, TestingModule } from '@nestjs/testing';
import request from 'supertest';
import { App } from 'supertest/types';

import { JwtAuthGuard } from './auth/jwt-auth.guard';
import { RolesGuard } from './auth/roles.guard';
import { HttpExceptionFilter } from './filters/http-exception.filter';
import { IncidentesController } from './incidentes.controller';
import { IncidentesService } from './incidentes.service';
import { PrismaService } from './prisma/prisma.service';

const JWT_SECRET = 'test-jwt-secret';
const USUARIO_ID = 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12';
const ESTADO_ID = 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14';
const CATEGORIA_ID = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11';

describe('IncidentesController - filtros de consulta (integracion)', () => {
  let app: INestApplication<App>;
  let token: string;
  const prisma = {
    incidente: {
      findMany: jest.fn(),
    },
  };

  const getIncidentes = (
    query: Record<string, string> = {},
    withToken = true,
  ) => {
    const peticion = request(app.getHttpServer())
      .get('/incidentes')
      .query(query);
    return withToken
      ? peticion.set('Authorization', `Bearer ${token}`)
      : peticion;
  };

  beforeEach(async () => {
    jest.resetAllMocks();
    const jwt = new JwtService({ secret: JWT_SECRET });
    token = jwt.sign({ sub: USUARIO_ID, roles: ['CIUDADANO'] });
    prisma.incidente.findMany.mockResolvedValue([]);

    const moduleFixture: TestingModule = await Test.createTestingModule({
      controllers: [IncidentesController],
      providers: [
        IncidentesService,
        { provide: PrismaService, useValue: prisma },
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

  it('responde 401 sin autenticacion y no consulta incidentes', async () => {
    await getIncidentes({}, false).expect(401);
    expect(prisma.incidente.findMany).not.toHaveBeenCalled();
  });

  it('sin filtros conserva where undefined, orden y estructura de arreglo', async () => {
    const respuesta = await getIncidentes().expect(200);
    expect(respuesta.body).toEqual([]);
    const args = prisma.incidente.findMany.mock.calls[0][0];
    expect(args.where).toBeUndefined();
    expect(args.orderBy).toEqual({ fechaCreacion: 'desc' });
    expect(args.include).toEqual(
      expect.objectContaining({
        categoria: true,
        estado: true,
        ubicacion: true,
      }),
    );
  });

  it('filtra por estado', async () => {
    await getIncidentes({ estadoId: ESTADO_ID }).expect(200);
    expect(prisma.incidente.findMany).toHaveBeenCalledWith(
      expect.objectContaining({
        where: { estadoId: ESTADO_ID },
      }),
    );
  });

  it('filtra por categoria', async () => {
    await getIncidentes({ categoriaId: CATEGORIA_ID }).expect(200);
    expect(prisma.incidente.findMany).toHaveBeenCalledWith(
      expect.objectContaining({
        where: { categoriaId: CATEGORIA_ID },
      }),
    );
  });

  it('filtra por comuna sin distinguir mayusculas', async () => {
    await getIncidentes({ comuna: '  TEMUCO  ' }).expect(200);
    expect(prisma.incidente.findMany).toHaveBeenCalledWith(
      expect.objectContaining({
        where: {
          ubicacion: {
            is: {
              comuna: { equals: 'TEMUCO', mode: 'insensitive' },
            },
          },
        },
      }),
    );
  });

  it('combina filtros y usuarioReportaId mediante AND', async () => {
    await getIncidentes({
      usuarioReportaId: USUARIO_ID,
      estadoId: ESTADO_ID,
      categoriaId: CATEGORIA_ID,
      comuna: 'Temuco',
    }).expect(200);

    expect(prisma.incidente.findMany).toHaveBeenCalledWith(
      expect.objectContaining({
        where: {
          usuarioReportaId: USUARIO_ID,
          estadoId: ESTADO_ID,
          categoriaId: CATEGORIA_ID,
          ubicacion: {
            is: {
              comuna: { equals: 'Temuco', mode: 'insensitive' },
            },
          },
        },
      }),
    );
  });

  it('devuelve una coleccion vacia cuando no hay coincidencias', async () => {
    const respuesta = await getIncidentes({
      estadoId: ESTADO_ID,
      comuna: 'Sin coincidencias',
    }).expect(200);

    expect(respuesta.body).toEqual([]);
  });

  it.each([
    { estadoId: 'no-es-uuid' },
    { categoriaId: 'no-es-uuid' },
    { usuarioReportaId: 'no-es-uuid' },
    { comuna: '   ' },
  ])(
    'responde 400 si un filtro tiene formato invalido: %o',
    async (query) => {
      await getIncidentes(query).expect(400);
      expect(prisma.incidente.findMany).not.toHaveBeenCalled();
    },
  );
});