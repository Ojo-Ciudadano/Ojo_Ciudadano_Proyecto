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
const CATEGORIA_ID = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11';
const USUARIO_ID = 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12';
const INCIDENTE_ID = 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13';
const ESTADO_ID = 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14';

const INCIDENTE_BODY = {
  titulo: 'Fuga de agua',
  descripcion: 'Fuga en la via publica',
  categoriaId: CATEGORIA_ID,
  usuarioReportaId: USUARIO_ID,
  ubicacion: {
    latitud: -38.7359,
    longitud: -72.5904,
    direccionReferencia: 'Av. Alemania 0211',
    comuna: 'Temuco',
    region: 'Araucania',
    sector: 'Centro',
  },
};

describe('IncidentesController - prioridad (integracion)', () => {
  let app: INestApplication<App>;
  let tokenAdmin: string;
  let tokenCiudadano: string;

  const tx = {
    ubicacion: { create: jest.fn() },
    incidente: { create: jest.fn() },
    historialEstado: { create: jest.fn() },
  };

  const prisma = {
    categoria: { findUnique: jest.fn() },
    estado: { findFirst: jest.fn() },
    incidente: { findUnique: jest.fn(), update: jest.fn() },
    $transaction: jest.fn(),
  };

  const patchPrioridad = (id: string, body: object, token?: string) => {
    const peticion = request(app.getHttpServer())
      .patch(`/incidentes/${id}/prioridad`)
      .send(body);

    return token ? peticion.set('Authorization', `Bearer ${token}`) : peticion;
  };

  beforeEach(async () => {
    jest.resetAllMocks();

    const jwt = new JwtService({ secret: JWT_SECRET });
    tokenAdmin = jwt.sign({ sub: USUARIO_ID, roles: ['ADMIN'] });
    tokenCiudadano = jwt.sign({ sub: USUARIO_ID, roles: ['CIUDADANO'] });

    prisma.categoria.findUnique.mockResolvedValue({
      id: CATEGORIA_ID,
      nombre: 'Agua y alcantarillado',
      prioridadBase: 3,
    });
    prisma.estado.findFirst.mockResolvedValue({
      id: ESTADO_ID,
      nombre: 'Nuevo',
    });
    prisma.$transaction.mockImplementation(
      async (callback: (tx: unknown) => unknown) => callback(tx),
    );
    tx.ubicacion.create.mockResolvedValue({ id: 'ubicacion-1' });
    tx.incidente.create.mockImplementation(
      async ({ data }: { data: object }) => ({ id: INCIDENTE_ID, ...data }),
    );
    tx.historialEstado.create.mockResolvedValue({ id: 'historial-1' });

    prisma.incidente.findUnique.mockResolvedValue({ id: INCIDENTE_ID });
    prisma.incidente.update.mockImplementation(
      async ({ data }: { data: { nivelPrioridad: number } }) => ({
        id: INCIDENTE_ID,
        nivelPrioridad: data.nivelPrioridad,
      }),
    );

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

  describe('POST /incidentes', () => {
    it('persiste la prioridadBase de la categoria como prioridad inicial', async () => {
      const respuesta = await request(app.getHttpServer())
        .post('/incidentes')
        .set('Authorization', `Bearer ${tokenCiudadano}`)
        .send(INCIDENTE_BODY)
        .expect(201);

      expect(respuesta.body.nivelPrioridad).toBe(3);
      expect(tx.incidente.create).toHaveBeenCalledWith(
        expect.objectContaining({
          data: expect.objectContaining({ nivelPrioridad: 3 }),
        }),
      );
    });

    it.each([
      ['nivelPrioridad', { ...INCIDENTE_BODY, nivelPrioridad: 5 }],
      ['prioridad', { ...INCIDENTE_BODY, prioridad: 5 }],
    ])(
      'rechaza con 400 cuando el cliente intenta enviar %s',
      async (_campo, body) => {
        const respuesta = await request(app.getHttpServer())
          .post('/incidentes')
          .set('Authorization', `Bearer ${tokenCiudadano}`)
          .send(body)
          .expect(400);

        expect(JSON.stringify(respuesta.body.message)).toContain('should not exist');
        expect(tx.incidente.create).not.toHaveBeenCalled();
      },
    );

    it('rechaza con 400 una categoria inexistente sin crear el incidente', async () => {
      prisma.categoria.findUnique.mockResolvedValue(null);

      await request(app.getHttpServer())
        .post('/incidentes')
        .set('Authorization', `Bearer ${tokenCiudadano}`)
        .send(INCIDENTE_BODY)
        .expect(400);

      expect(tx.incidente.create).not.toHaveBeenCalled();
    });

    it('rechaza con 400 una categoria con prioridad base invalida', async () => {
      prisma.categoria.findUnique.mockResolvedValue({
        id: CATEGORIA_ID,
        nombre: 'Categoria inconsistente',
        prioridadBase: 9,
      });

      await request(app.getHttpServer())
        .post('/incidentes')
        .set('Authorization', `Bearer ${tokenCiudadano}`)
        .send(INCIDENTE_BODY)
        .expect(400);

      expect(tx.incidente.create).not.toHaveBeenCalled();
    });
  });

  describe('PATCH /incidentes/:id/prioridad', () => {
    it('responde 401 si no hay autenticacion valida', async () => {
      await patchPrioridad(INCIDENTE_ID, { nivelPrioridad: 4 }).expect(401);

      expect(prisma.incidente.update).not.toHaveBeenCalled();
    });

    it('responde 401 si el token es invalido', async () => {
      await patchPrioridad(INCIDENTE_ID, { nivelPrioridad: 4 }, 'token-invalido').expect(401);

      expect(prisma.incidente.update).not.toHaveBeenCalled();
    });

    it('responde 403 si el usuario autenticado es Ciudadano', async () => {
      await patchPrioridad(
        INCIDENTE_ID,
        { nivelPrioridad: 4 },
        tokenCiudadano,
      ).expect(403);

      expect(prisma.incidente.update).not.toHaveBeenCalled();
    });

    it('permite al Administrador modificar la prioridad y confirma el valor', async () => {
      const respuesta = await patchPrioridad(
        INCIDENTE_ID,
        { nivelPrioridad: 4 },
        tokenAdmin,
      ).expect(200);

      expect(respuesta.body.nivelPrioridad).toBe(4);
      expect(prisma.incidente.update).toHaveBeenCalledWith({
        where: { id: INCIDENTE_ID },
        data: { nivelPrioridad: 4 },
        include: expect.objectContaining({
          categoria: true,
          estado: true,
          ubicacion: true,
        }),
      });
    });

    it('no altera categoria, estado ni asignacion del incidente', async () => {
      await patchPrioridad(INCIDENTE_ID, { nivelPrioridad: 2 }, tokenAdmin).expect(200);

      const data = prisma.incidente.update.mock.calls[0][0].data;

      expect(Object.keys(data)).toEqual(['nivelPrioridad']);
    });

    it('responde 400 si el id no es un UUID valido', async () => {
      const respuesta = await patchPrioridad(
        'no-es-un-uuid',
        { nivelPrioridad: 4 },
        tokenAdmin,
      ).expect(400);

      expect(JSON.stringify(respuesta.body.message)).toContain('UUID');
      expect(prisma.incidente.update).not.toHaveBeenCalled();
    });

    it('responde 404 si el incidente no existe', async () => {
      prisma.incidente.findUnique.mockResolvedValue(null);

      await patchPrioridad(INCIDENTE_ID, { nivelPrioridad: 4 }, tokenAdmin).expect(404);

      expect(prisma.incidente.update).not.toHaveBeenCalled();
    });

    it.each([
      ['cero', 0],
      ['sobre la escala', 6],
      ['decimal', 2.5],
      ['texto', 'alta'],
      ['ausente', undefined],
    ])('responde 400 si la prioridad es %s', async (_caso, nivelPrioridad) => {
      await patchPrioridad(INCIDENTE_ID, { nivelPrioridad }, tokenAdmin).expect(400);

      expect(prisma.incidente.update).not.toHaveBeenCalled();
    });

    it('responde 400 si se intenta modificar otro campo del incidente', async () => {
      await patchPrioridad(
        INCIDENTE_ID,
        { nivelPrioridad: 4, categoriaId: CATEGORIA_ID },
        tokenAdmin,
      ).expect(400);

      expect(prisma.incidente.update).not.toHaveBeenCalled();
    });
  });
});
