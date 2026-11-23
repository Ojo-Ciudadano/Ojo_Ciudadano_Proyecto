/// <reference types="jest" />
import {
  INestApplication,
  ValidationPipe,
} from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { Test, TestingModule } from '@nestjs/testing';
import request from 'supertest';
import { App } from 'supertest/types';

import { NotificacionesService } from '../../application/services/notificaciones.service';
import { JwtAuthGuard } from '../../infrastructure/security/jwt-auth.guard';
import { PrismaService } from '../../infrastructure/persistence/prisma/prisma.service';
import { HttpExceptionFilter } from '../filters/http-exception.filter';
import { NotificacionesController } from './notificaciones.controller';

const JWT_SECRET = 'test-jwt-secret';
const USUARIO_ID = 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380b22';
const INCIDENTE_ID = 'c2eebc99-9c0b-4ef8-bb6d-6bb9bd380c33';
const NOTIFICACION_PROPIA_ID = '30000000-0000-4000-8000-000000000001';
const NOTIFICACION_AJENA_ID = '30000000-0000-4000-8000-000000000002';

const notificacion = (id: string, usuarioId: string, leido: boolean) => ({
  id,
  usuarioId,
  incidenteId: INCIDENTE_ID,
  mensaje: 'El incidente cambió de estado',
  leido,
  fecha: new Date('2026-10-30T01:00:00.000Z'),
});

describe('NotificacionesController - marcar como leídas (#327)', () => {
  let app: INestApplication<App>;
  let token: string;

  const prisma = {
    notificacion: {
      create: jest.fn(),
      findMany: jest.fn(),
      findFirst: jest.fn(),
      update: jest.fn(),
      updateMany: jest.fn(),
    },
  };

  const patchLeida = (id: string, withToken = true) => {
    const peticion = request(app.getHttpServer()).patch(
      `/notificaciones/${id}/leida`,
    );
    return withToken
      ? peticion.set('Authorization', `Bearer ${token}`)
      : peticion;
  };

  beforeEach(async () => {
    jest.resetAllMocks();

    const jwt = new JwtService({ secret: JWT_SECRET });
    token = jwt.sign({ sub: USUARIO_ID });

    prisma.notificacion.findFirst.mockImplementation(
      async ({ where }: { where: { id: string; usuarioId: string } }) =>
        where.id === NOTIFICACION_PROPIA_ID &&
        where.usuarioId === USUARIO_ID
          ? notificacion(NOTIFICACION_PROPIA_ID, USUARIO_ID, false)
          : null,
    );
    prisma.notificacion.update.mockImplementation(
      async ({ where, data }: { where: { id: string }; data: { leido: boolean } }) =>
        notificacion(where.id, USUARIO_ID, data.leido),
    );
    prisma.notificacion.updateMany.mockResolvedValue({ count: 2 });

    const moduleFixture: TestingModule = await Test.createTestingModule({
      controllers: [NotificacionesController],
      providers: [
        NotificacionesService,
        JwtAuthGuard,
        { provide: PrismaService, useValue: prisma },
        { provide: JwtService, useValue: jwt },
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
    await app?.close();
  });

  it('marca como leída una notificación propia y devuelve el DTO actualizado', async () => {
    const respuesta = await patchLeida(NOTIFICACION_PROPIA_ID).expect(200);

    expect(prisma.notificacion.findFirst).toHaveBeenCalledWith({
      where: { id: NOTIFICACION_PROPIA_ID, usuarioId: USUARIO_ID },
    });
    expect(prisma.notificacion.update).toHaveBeenCalledWith({
      where: { id: NOTIFICACION_PROPIA_ID },
      data: { leido: true },
    });
    expect(respuesta.body).toMatchObject({
      id: NOTIFICACION_PROPIA_ID,
      usuarioId: USUARIO_ID,
      leido: true,
    });
  });

  it('conserva el estado leído al repetir la operación individual', async () => {
    const primera = await patchLeida(NOTIFICACION_PROPIA_ID).expect(200);
    const segunda = await patchLeida(NOTIFICACION_PROPIA_ID).expect(200);

    expect(primera.body.leido).toBe(true);
    expect(segunda.body.leido).toBe(true);
    expect(prisma.notificacion.update).toHaveBeenCalledTimes(2);
  });

  it('rechaza con 403 una notificación ajena sin modificarla', async () => {
    const respuesta = await patchLeida(NOTIFICACION_AJENA_ID).expect(403);

    expect(prisma.notificacion.findFirst).toHaveBeenCalledWith({
      where: { id: NOTIFICACION_AJENA_ID, usuarioId: USUARIO_ID },
    });
    expect(prisma.notificacion.update).not.toHaveBeenCalled();
    expect(respuesta.body).toMatchObject({ statusCode: 403 });
  });

  it('rechaza con 401 una solicitud sin sesión', async () => {
    await patchLeida(NOTIFICACION_PROPIA_ID, false).expect(401);
    expect(prisma.notificacion.findFirst).not.toHaveBeenCalled();
    expect(prisma.notificacion.update).not.toHaveBeenCalled();
  });

  it('rechaza con 400 un id que no es UUID', async () => {
    await patchLeida('no-uuid').expect(400);
    expect(prisma.notificacion.findFirst).not.toHaveBeenCalled();
  });

  it('marca todas las pendientes del usuario y es idempotente', async () => {
    prisma.notificacion.updateMany
      .mockResolvedValueOnce({ count: 2 })
      .mockResolvedValueOnce({ count: 0 });

    const primera = await request(app.getHttpServer())
      .patch('/notificaciones/leidas')
      .set('Authorization', `Bearer ${token}`)
      .expect(200);
    const segunda = await request(app.getHttpServer())
      .patch('/notificaciones/leidas')
      .set('Authorization', `Bearer ${token}`)
      .expect(200);

    expect(prisma.notificacion.updateMany).toHaveBeenCalledWith({
      where: { usuarioId: USUARIO_ID, leido: false },
      data: { leido: true },
    });
    expect(primera.body).toEqual({ marcadas: 2 });
    expect(segunda.body).toEqual({ marcadas: 0 });
  });

  it('rechaza con 401 la operación masiva sin sesión', async () => {
    await request(app.getHttpServer())
      .patch('/notificaciones/leidas')
      .expect(401);
    expect(prisma.notificacion.updateMany).not.toHaveBeenCalled();
  });
});
