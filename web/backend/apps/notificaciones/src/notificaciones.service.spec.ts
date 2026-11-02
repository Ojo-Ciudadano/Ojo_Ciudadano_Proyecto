import {
  BadRequestException,
  INestApplication,
  InternalServerErrorException,
  ValidationPipe,
} from '@nestjs/common';
import { Test, TestingModule } from '@nestjs/testing';
import request from 'supertest';
import { App } from 'supertest/types';

import { HttpExceptionFilter } from './filters/http-exception.filter';
import { NotificacionesController } from './notificaciones.controller';
import { NotificacionesService } from './notificaciones.service';
import { PrismaService } from './prisma/prisma.service';

const USUARIO_ID = 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380b22';
const INCIDENTE_ID = 'c2eebc99-9c0b-4ef8-bb6d-6bb9bd380c33';

describe('NotificacionesService y Controller (#113)', () => {
  let app: INestApplication<App>;
  let service: NotificacionesService;
  let prisma: { notificacion: { create: jest.Mock } };

  beforeEach(async () => {
    prisma = {
      notificacion: {
        create: jest.fn(async ({ data }: { data: Record<string, unknown> }) => ({
          id: '30000000-0000-4000-8000-000000000099',
          ...data,
          leido: false,
          fecha: new Date('2026-10-30T01:00:00.000Z'),
        })),
      },
    };

    const moduleFixture: TestingModule = await Test.createTestingModule({
      controllers: [NotificacionesController],
      providers: [
        NotificacionesService,
        { provide: PrismaService, useValue: prisma },
      ],
    }).compile();

    service = moduleFixture.get<NotificacionesService>(NotificacionesService);
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

  it('POST /notificaciones persiste la notificacion en la base de datos con fecha, usuarioId e incidenteId', async () => {
    const respuesta = await request(app.getHttpServer())
      .post('/notificaciones')
      .send({
        usuarioId: USUARIO_ID,
        incidenteId: INCIDENTE_ID,
        mensaje:
          '  Tu incidente "Alumbrado caído" cambió de estado de "Nuevo" a "En revisión".  ',
      })
      .expect(201);

    expect(prisma.notificacion.create).toHaveBeenCalledTimes(1);
    expect(prisma.notificacion.create).toHaveBeenCalledWith({
      data: {
        usuarioId: USUARIO_ID,
        incidenteId: INCIDENTE_ID,
        mensaje:
          'Tu incidente "Alumbrado caído" cambió de estado de "Nuevo" a "En revisión".',
      },
    });
    expect(respuesta.body).toMatchObject({
      id: '30000000-0000-4000-8000-000000000099',
      usuarioId: USUARIO_ID,
      incidenteId: INCIDENTE_ID,
      leido: false,
    });
    expect(respuesta.body.fecha).toBeDefined();
  });

  it('rechaza con 400 si usuarioId o incidenteId no son UUID validos', async () => {
    await request(app.getHttpServer())
      .post('/notificaciones')
      .send({
        usuarioId: 'no-uuid',
        incidenteId: INCIDENTE_ID,
        mensaje: 'Cambio de estado',
      })
      .expect(400);

    expect(prisma.notificacion.create).not.toHaveBeenCalled();
  });

  it('rechaza con 400 si el mensaje contiene solo espacios en blanco', async () => {
    await expect(
      service.create({
        usuarioId: USUARIO_ID,
        incidenteId: INCIDENTE_ID,
        mensaje: '   ',
      }),
    ).rejects.toBeInstanceOf(BadRequestException);

    expect(prisma.notificacion.create).not.toHaveBeenCalled();
  });

  it('traduce un fallo de base de datos a InternalServerErrorException controlada', async () => {
    prisma.notificacion.create.mockRejectedValue(
      new Error('Error de conexion PostgreSQL'),
    );

    await expect(
      service.create({
        usuarioId: USUARIO_ID,
        incidenteId: INCIDENTE_ID,
        mensaje: 'Evento de cambio de estado',
      }),
    ).rejects.toBeInstanceOf(InternalServerErrorException);
  });
});
