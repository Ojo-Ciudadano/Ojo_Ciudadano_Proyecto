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
const INCIDENTE_ID = 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13';
const USUARIO_ADMIN_ID = 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12';
const USUARIO_CIUDADANO_ID = 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a19';
const ESTADO_DESTINO_ID = 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a15';

describe('IncidentesController - cambio de estado (integracion)', () => {
  let app: INestApplication<App>;
  let tokenAdmin: string;
  let tokenCiudadano: string;

  const tx = {
    $queryRaw: jest.fn(),
    incidente: { findUnique: jest.fn(), update: jest.fn() },
    estado: { findUnique: jest.fn() },
    historialEstado: { create: jest.fn() },
  };

  const prisma = {
    $transaction: jest.fn(),
  };

  const patchEstado = (id: string, body: object, token?: string) => {
    const peticion = request(app.getHttpServer())
      .patch(`/incidentes/${id}/estado`)
      .send(body);

    return token ? peticion.set('Authorization', `Bearer ${token}`) : peticion;
  };

  beforeEach(async () => {
    jest.resetAllMocks();

    const jwt = new JwtService({ secret: JWT_SECRET });
    tokenAdmin = jwt.sign({ sub: USUARIO_ADMIN_ID, roles: ['ADMIN'] });
    tokenCiudadano = jwt.sign({
      sub: USUARIO_CIUDADANO_ID,
      roles: ['CIUDADANO'],
    });

    tx.$queryRaw.mockResolvedValue([{ id: INCIDENTE_ID }]);
    tx.incidente.findUnique.mockResolvedValue({
      id: INCIDENTE_ID,
      estado: { nombre: 'Nuevo' },
    });
    tx.estado.findUnique.mockImplementation(
      async ({ where }: { where: { nombre: string } }) => ({
        id: ESTADO_DESTINO_ID,
        nombre: where.nombre,
      }),
    );
    tx.incidente.update.mockImplementation(
      async ({ data }: { data: { estadoId: string } }) => ({
        id: INCIDENTE_ID,
        estadoId: data.estadoId,
        estado: { id: data.estadoId, nombre: 'En revisión' },
        evidencias: [],
      }),
    );
    tx.historialEstado.create.mockResolvedValue({ id: 'historial-1' });

    prisma.$transaction.mockImplementation(
      async (callback: (tx: unknown) => unknown) => callback(tx),
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

  it('responde 401 si no hay autenticacion valida', async () => {
    await patchEstado(INCIDENTE_ID, {
      estado: 'En revisión',
      observacion: 'Evaluacion',
    }).expect(401);

    expect(prisma.$transaction).not.toHaveBeenCalled();
  });

  it('responde 401 si el token es invalido', async () => {
    await patchEstado(
      INCIDENTE_ID,
      { estado: 'En revisión', observacion: 'Evaluacion' },
      'token-invalido',
    ).expect(401);

    expect(prisma.$transaction).not.toHaveBeenCalled();
  });

  it('responde 403 si el usuario autenticado es Ciudadano', async () => {
    await patchEstado(
      INCIDENTE_ID,
      { estado: 'En revisión', observacion: 'Evaluacion' },
      tokenCiudadano,
    ).expect(403);

    expect(prisma.$transaction).not.toHaveBeenCalled();
  });

  it('permite al Administrador ejecutar una transicion valida y confirma el estado', async () => {
    const respuesta = await patchEstado(
      INCIDENTE_ID,
      { estado: 'En revisión', observacion: '  Se inicia la evaluacion  ' },
      tokenAdmin,
    ).expect(200);

    expect(respuesta.body.id).toBe(INCIDENTE_ID);
    expect(respuesta.body.estado.nombre).toBe('En revisión');
    expect(tx.incidente.update).toHaveBeenCalledWith({
      where: { id: INCIDENTE_ID },
      data: { estadoId: ESTADO_DESTINO_ID },
      include: expect.objectContaining({
        categoria: true,
        estado: true,
        ubicacion: true,
      }),
    });
    expect(tx.historialEstado.create).toHaveBeenCalledTimes(1);
    expect(tx.historialEstado.create).toHaveBeenCalledWith({
      data: {
        incidenteId: INCIDENTE_ID,
        estadoId: ESTADO_DESTINO_ID,
        usuarioId: USUARIO_ADMIN_ID,
        observacion: 'Se inicia la evaluacion',
      },
    });
  });

  it('rechaza con 400 un identificador de usuario enviado por el cliente', async () => {
    await patchEstado(
      INCIDENTE_ID,
      {
        estado: 'En revisión',
        observacion: 'Evaluacion',
        usuarioId: USUARIO_CIUDADANO_ID,
      },
      tokenAdmin,
    ).expect(400);

    expect(prisma.$transaction).not.toHaveBeenCalled();
  });

  it('responde 400 si el id del incidente no es un UUID valido', async () => {
    const respuesta = await patchEstado(
      'no-es-un-uuid',
      { estado: 'En revisión', observacion: 'Evaluacion' },
      tokenAdmin,
    ).expect(400);

    expect(JSON.stringify(respuesta.body.message)).toContain('UUID');
    expect(prisma.$transaction).not.toHaveBeenCalled();
  });

  it.each(['Pendiente', 'En revision', ''])(
    'responde 400 si el estado solicitado no es canonico (%s)',
    async (estado) => {
      await patchEstado(
        INCIDENTE_ID,
        { estado, observacion: 'Evaluacion' },
        tokenAdmin,
      ).expect(400);

      expect(prisma.$transaction).not.toHaveBeenCalled();
    },
  );

  it('responde 400 si falta la observacion', async () => {
    await patchEstado(
      INCIDENTE_ID,
      { estado: 'En revisión' },
      tokenAdmin,
    ).expect(400);

    expect(prisma.$transaction).not.toHaveBeenCalled();
  });

  it('responde 400 si la observacion esta vacia o solo tiene espacios', async () => {
    await patchEstado(
      INCIDENTE_ID,
      { estado: 'En revisión', observacion: '   ' },
      tokenAdmin,
    ).expect(400);

    expect(prisma.$transaction).not.toHaveBeenCalled();
  });

  it('responde 404 si el incidente no existe sin generar cambios ni historial', async () => {
    tx.$queryRaw.mockResolvedValue([]);

    await patchEstado(
      INCIDENTE_ID,
      { estado: 'En revisión', observacion: 'Evaluacion' },
      tokenAdmin,
    ).expect(404);

    expect(tx.incidente.update).not.toHaveBeenCalled();
    expect(tx.historialEstado.create).not.toHaveBeenCalled();
  });

  it('responde 404 si el estado destino no existe sin generar cambios ni historial', async () => {
    tx.estado.findUnique.mockResolvedValue(null);

    await patchEstado(
      INCIDENTE_ID,
      { estado: 'En revisión', observacion: 'Evaluacion' },
      tokenAdmin,
    ).expect(404);

    expect(tx.incidente.update).not.toHaveBeenCalled();
    expect(tx.historialEstado.create).not.toHaveBeenCalled();
  });

  it('responde 409 ante una transicion prohibida y conserva estado e historial', async () => {
    tx.incidente.findUnique.mockResolvedValue({
      id: INCIDENTE_ID,
      estado: { nombre: 'Nuevo' },
    });

    await patchEstado(
      INCIDENTE_ID,
      { estado: 'Cerrado', observacion: 'Intento invalido' },
      tokenAdmin,
    ).expect(409);

    expect(tx.incidente.update).not.toHaveBeenCalled();
    expect(tx.historialEstado.create).not.toHaveBeenCalled();
  });

  it('responde 409 al solicitar el mismo estado y no registra historial', async () => {
    tx.incidente.findUnique.mockResolvedValue({
      id: INCIDENTE_ID,
      estado: { nombre: 'En revisión' },
    });

    await patchEstado(
      INCIDENTE_ID,
      { estado: 'En revisión', observacion: 'Mismo estado' },
      tokenAdmin,
    ).expect(409);

    expect(tx.incidente.update).not.toHaveBeenCalled();
    expect(tx.historialEstado.create).not.toHaveBeenCalled();
  });
});
