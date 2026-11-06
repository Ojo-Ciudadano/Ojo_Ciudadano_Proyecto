import { HttpException, INestApplication, ValidationPipe } from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import { JwtService } from '@nestjs/jwt';
import { Test, TestingModule } from '@nestjs/testing';
import request from 'supertest';
import { App } from 'supertest/types';

import { AuthUsuariosClient } from './auth/auth-usuarios.client';
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
const USUARIO_ASIGNADO_ID = 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a31';
const OTRO_USUARIO_ID = 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a32';
const ESTADO_ID = 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14';
const ASIGNACION_ID = 'f0eebc99-9c0b-4ef8-bb6d-6bb9bd380a41';
const FECHA = new Date('2026-10-30T10:00:00.000Z');

describe('IncidentesController - asignacion de responsable (integracion)', () => {
  let app: INestApplication<App>;
  let tokenAdmin: string;
  let tokenCiudadano: string;

  const tx = {
    $queryRaw: jest.fn(),
    incidente: { findUnique: jest.fn() },
    asignacion: {
      findFirst: jest.fn(),
      update: jest.fn(),
      create: jest.fn(),
    },
    historialEstado: { create: jest.fn() },
  };

  const prisma = {
    $transaction: jest.fn(),
  };

  const authUsuariosClient = {
    obtenerUsuarioConRoles: jest.fn(),
  };

  const patchAsignacion = (id: string, body: object, token?: string) => {
    const peticion = request(app.getHttpServer())
      .patch(`/incidentes/${id}/asignacion`)
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

    authUsuariosClient.obtenerUsuarioConRoles.mockResolvedValue({
      id: USUARIO_ASIGNADO_ID,
      nombre: 'Ana Admin',
      email: 'ana@example.com',
      roles: ['ADMIN'],
    });

    tx.$queryRaw.mockResolvedValue([{ id: INCIDENTE_ID }]);
    tx.incidente.findUnique.mockResolvedValue({
      id: INCIDENTE_ID,
      estadoId: ESTADO_ID,
    });
    tx.asignacion.findFirst.mockResolvedValue(null);
    tx.asignacion.update.mockResolvedValue({});
    tx.asignacion.create.mockImplementation(
      async ({ data }: { data: Record<string, unknown> }) => ({
        id: ASIGNACION_ID,
        fechaAsignacion: FECHA,
        ...data,
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
        { provide: AuthUsuariosClient, useValue: authUsuariosClient },
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
    await app?.close();
  });

  describe('control de acceso', () => {
    it('responde 401 si no hay autenticacion valida', async () => {
      await patchAsignacion(INCIDENTE_ID, {
        usuarioAsignadoId: USUARIO_ASIGNADO_ID,
      }).expect(401);

      expect(authUsuariosClient.obtenerUsuarioConRoles).not.toHaveBeenCalled();
      expect(prisma.$transaction).not.toHaveBeenCalled();
    });

    it('responde 401 si el token es invalido', async () => {
      await patchAsignacion(
        INCIDENTE_ID,
        { usuarioAsignadoId: USUARIO_ASIGNADO_ID },
        'token-invalido',
      ).expect(401);

      expect(authUsuariosClient.obtenerUsuarioConRoles).not.toHaveBeenCalled();
      expect(prisma.$transaction).not.toHaveBeenCalled();
    });

    it('responde 403 si el usuario autenticado es Ciudadano', async () => {
      await patchAsignacion(
        INCIDENTE_ID,
        { usuarioAsignadoId: USUARIO_ASIGNADO_ID },
        tokenCiudadano,
      ).expect(403);

      expect(authUsuariosClient.obtenerUsuarioConRoles).not.toHaveBeenCalled();
      expect(prisma.$transaction).not.toHaveBeenCalled();
    });
  });

  describe('asignacion inicial', () => {
    it('permite al Administrador asignar un responsable y confirma la persistencia', async () => {
      const respuesta = await patchAsignacion(
        INCIDENTE_ID,
        { usuarioAsignadoId: USUARIO_ASIGNADO_ID },
        tokenAdmin,
      ).expect(200);

      expect(respuesta.body).toEqual({
        id: ASIGNACION_ID,
        incidenteId: INCIDENTE_ID,
        usuarioAsignadoId: USUARIO_ASIGNADO_ID,
        asignadoPorId: USUARIO_ADMIN_ID,
        fechaAsignacion: FECHA.toISOString(),
        activa: true,
      });

      expect(tx.$queryRaw).toHaveBeenCalledTimes(1);
      expect(tx.asignacion.update).not.toHaveBeenCalled();
      expect(tx.asignacion.create).toHaveBeenCalledWith({
        data: {
          incidenteId: INCIDENTE_ID,
          usuarioAsignadoId: USUARIO_ASIGNADO_ID,
          asignadoPorId: USUARIO_ADMIN_ID,
          activa: true,
        },
      });
      expect(tx.historialEstado.create).toHaveBeenCalledTimes(1);
      expect(tx.historialEstado.create).toHaveBeenCalledWith({
        data: {
          incidenteId: INCIDENTE_ID,
          estadoId: ESTADO_ID,
          usuarioId: USUARIO_ADMIN_ID,
          observacion: `Asignado a ${USUARIO_ASIGNADO_ID}`,
        },
      });
    });

    it('reenvia a auth-usuarios el token Bearer del actor', async () => {
      await patchAsignacion(
        INCIDENTE_ID,
        { usuarioAsignadoId: USUARIO_ASIGNADO_ID },
        tokenAdmin,
      ).expect(200);

      expect(authUsuariosClient.obtenerUsuarioConRoles).toHaveBeenCalledWith(
        USUARIO_ASIGNADO_ID,
        `Bearer ${tokenAdmin}`,
      );
    });
  });

  describe('reasignacion e idempotencia', () => {
    it('desactiva la asignacion vigente y registra la reasignacion', async () => {
      const vigente = {
        id: 'f0eebc99-9c0b-4ef8-bb6d-6bb9bd380a40',
        incidenteId: INCIDENTE_ID,
        usuarioAsignadoId: OTRO_USUARIO_ID,
        asignadoPorId: USUARIO_ADMIN_ID,
        fechaAsignacion: new Date('2026-10-01T10:00:00.000Z'),
        activa: true,
      };
      tx.asignacion.findFirst.mockResolvedValue(vigente);

      const respuesta = await patchAsignacion(
        INCIDENTE_ID,
        { usuarioAsignadoId: USUARIO_ASIGNADO_ID },
        tokenAdmin,
      ).expect(200);

      expect(respuesta.body.usuarioAsignadoId).toBe(USUARIO_ASIGNADO_ID);
      expect(respuesta.body.activa).toBe(true);
      expect(tx.asignacion.update).toHaveBeenCalledWith({
        where: { id: vigente.id },
        data: { activa: false },
      });
      expect(tx.asignacion.create).toHaveBeenCalledTimes(1);
      expect(
        tx.asignacion.update.mock.invocationCallOrder[0],
      ).toBeLessThan(tx.asignacion.create.mock.invocationCallOrder[0]);
      expect(tx.historialEstado.create).toHaveBeenCalledWith({
        data: {
          incidenteId: INCIDENTE_ID,
          estadoId: ESTADO_ID,
          usuarioId: USUARIO_ADMIN_ID,
          observacion: `Reasignado de ${OTRO_USUARIO_ID} a ${USUARIO_ASIGNADO_ID}`,
        },
      });
    });

    it('devuelve la asignacion vigente sin mutar nada si ya esta asignada al mismo usuario', async () => {
      tx.asignacion.findFirst.mockResolvedValue({
        id: ASIGNACION_ID,
        incidenteId: INCIDENTE_ID,
        usuarioAsignadoId: USUARIO_ASIGNADO_ID,
        asignadoPorId: USUARIO_ADMIN_ID,
        fechaAsignacion: FECHA,
        activa: true,
      });

      const respuesta = await patchAsignacion(
        INCIDENTE_ID,
        { usuarioAsignadoId: USUARIO_ASIGNADO_ID },
        tokenAdmin,
      ).expect(200);

      expect(respuesta.body.id).toBe(ASIGNACION_ID);
      expect(tx.asignacion.update).not.toHaveBeenCalled();
      expect(tx.asignacion.create).not.toHaveBeenCalled();
      expect(tx.historialEstado.create).not.toHaveBeenCalled();
    });
  });

  describe('validacion de entrada', () => {
    it('responde 400 si el id del incidente no es un UUID valido', async () => {
      const respuesta = await patchAsignacion(
        'no-es-un-uuid',
        { usuarioAsignadoId: USUARIO_ASIGNADO_ID },
        tokenAdmin,
      ).expect(400);

      expect(JSON.stringify(respuesta.body.message)).toContain('UUID');
      expect(authUsuariosClient.obtenerUsuarioConRoles).not.toHaveBeenCalled();
      expect(prisma.$transaction).not.toHaveBeenCalled();
    });

    it.each([
      [{ usuarioAsignadoId: 'no-es-uuid' }],
      [{}],
      [{ usuarioAsignadoId: '' }],
    ])(
      'responde 400 si el usuario asignado es invalido o falta: %o',
      async (body) => {
        await patchAsignacion(INCIDENTE_ID, body, tokenAdmin).expect(400);

        expect(
          authUsuariosClient.obtenerUsuarioConRoles,
        ).not.toHaveBeenCalled();
        expect(prisma.$transaction).not.toHaveBeenCalled();
      },
    );

    it('rechaza con 400 un actor enviado por el cliente (la identidad sale del token)', async () => {
      await patchAsignacion(
        INCIDENTE_ID,
        {
          usuarioAsignadoId: USUARIO_ASIGNADO_ID,
          asignadoPorId: USUARIO_CIUDADANO_ID,
        },
        tokenAdmin,
      ).expect(400);

      expect(prisma.$transaction).not.toHaveBeenCalled();
    });
  });

  describe('validacion con auth-usuarios', () => {
    it('responde 400 si el usuario propuesto no tiene rol ADMIN', async () => {
      authUsuariosClient.obtenerUsuarioConRoles.mockResolvedValue({
        id: USUARIO_ASIGNADO_ID,
        nombre: 'Carlos Ciudadano',
        email: 'carlos@example.com',
        roles: ['CIUDADANO'],
      });

      await patchAsignacion(
        INCIDENTE_ID,
        { usuarioAsignadoId: USUARIO_ASIGNADO_ID },
        tokenAdmin,
      ).expect(400);

      expect(prisma.$transaction).not.toHaveBeenCalled();
      expect(tx.asignacion.create).not.toHaveBeenCalled();
      expect(tx.historialEstado.create).not.toHaveBeenCalled();
    });

    it.each([
      [404, 'Usuario no encontrado'],
      [503, 'Servicio de usuarios no disponible'],
    ])(
      'propaga el %i de auth-usuarios sin iniciar transaccion ni mutar estado',
      async (status, mensaje) => {
        authUsuariosClient.obtenerUsuarioConRoles.mockRejectedValue(
          new HttpException(mensaje, status),
        );

        const respuesta = await patchAsignacion(
          INCIDENTE_ID,
          { usuarioAsignadoId: USUARIO_ASIGNADO_ID },
          tokenAdmin,
        ).expect(status);

        expect(respuesta.body.message).toBe(mensaje);
        expect(prisma.$transaction).not.toHaveBeenCalled();
        expect(tx.asignacion.update).not.toHaveBeenCalled();
        expect(tx.asignacion.create).not.toHaveBeenCalled();
        expect(tx.historialEstado.create).not.toHaveBeenCalled();
      },
    );
  });

  describe('incidente inexistente', () => {
    it('responde 404 si el incidente no existe sin generar cambios ni historial', async () => {
      tx.$queryRaw.mockResolvedValue([]);

      await patchAsignacion(
        INCIDENTE_ID,
        { usuarioAsignadoId: USUARIO_ASIGNADO_ID },
        tokenAdmin,
      ).expect(404);

      expect(tx.asignacion.update).not.toHaveBeenCalled();
      expect(tx.asignacion.create).not.toHaveBeenCalled();
      expect(tx.historialEstado.create).not.toHaveBeenCalled();
    });
  });
});
