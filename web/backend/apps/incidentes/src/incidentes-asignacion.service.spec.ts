import {
  BadRequestException,
  HttpException,
  NotFoundException,
  UnauthorizedException,
} from '@nestjs/common';
import { AuthUsuariosClient } from './auth/auth-usuarios.client';
import { IncidentesService } from './incidentes.service';
import { PrismaService } from './prisma/prisma.service';

const INCIDENTE_ID = 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13';
const ACTOR_ID = 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12';
const USUARIO_ASIGNADO_ID = 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a31';
const OTRO_USUARIO_ID = 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a32';
const ESTADO_ID = 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14';
const ASIGNACION_ID = 'f0eebc99-9c0b-4ef8-bb6d-6bb9bd380a41';
const FECHA = new Date('2026-10-30T10:00:00.000Z');
const AUTH_HEADER = 'Bearer token-del-actor';

describe('IncidentesService - asignarResponsable (#123)', () => {
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

  let service: IncidentesService;

  const asignar = (
    dto: { usuarioAsignadoId: string } = {
      usuarioAsignadoId: USUARIO_ASIGNADO_ID,
    },
    id: string = INCIDENTE_ID,
    actorId: string = ACTOR_ID,
    authHeader: string | undefined = AUTH_HEADER,
  ) => service.asignarResponsable(id, dto, actorId, authHeader);

  beforeEach(() => {
    jest.resetAllMocks();

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

    service = new IncidentesService(
      prisma as unknown as PrismaService,
      authUsuariosClient as unknown as AuthUsuariosClient,
    );
  });

  describe('asignacion inicial', () => {
    it('bloquea el incidente con SELECT ... FOR UPDATE dentro de la transaccion', async () => {
      await asignar();

      expect(prisma.$transaction).toHaveBeenCalledTimes(1);
      expect(tx.$queryRaw).toHaveBeenCalledTimes(1);

      const [plantilla, idBloqueado] = tx.$queryRaw.mock.calls[0];
      expect((plantilla as string[]).join(' ')).toMatch(/FOR UPDATE/);
      expect(idBloqueado).toBe(INCIDENTE_ID);
    });

    it('crea la asignacion activa y devuelve el contrato de respuesta', async () => {
      const resultado = await asignar();

      expect(tx.asignacion.update).not.toHaveBeenCalled();
      expect(tx.asignacion.create).toHaveBeenCalledTimes(1);
      expect(tx.asignacion.create).toHaveBeenCalledWith({
        data: {
          incidenteId: INCIDENTE_ID,
          usuarioAsignadoId: USUARIO_ASIGNADO_ID,
          asignadoPorId: ACTOR_ID,
          activa: true,
        },
      });
      expect(resultado).toEqual({
        id: ASIGNACION_ID,
        incidenteId: INCIDENTE_ID,
        usuarioAsignadoId: USUARIO_ASIGNADO_ID,
        asignadoPorId: ACTOR_ID,
        fechaAsignacion: FECHA,
        activa: true,
      });
    });

    it('registra un historial con el estado actual, el actor y la observacion', async () => {
      await asignar();

      expect(tx.historialEstado.create).toHaveBeenCalledTimes(1);
      expect(tx.historialEstado.create).toHaveBeenCalledWith({
        data: {
          incidenteId: INCIDENTE_ID,
          estadoId: ESTADO_ID,
          usuarioId: ACTOR_ID,
          observacion: `Asignado a ${USUARIO_ASIGNADO_ID}`,
        },
      });
    });

    it('valida el usuario propuesto en auth-usuarios reenviando el header de autorizacion', async () => {
      await asignar();

      expect(authUsuariosClient.obtenerUsuarioConRoles).toHaveBeenCalledTimes(1);
      expect(authUsuariosClient.obtenerUsuarioConRoles).toHaveBeenCalledWith(
        USUARIO_ASIGNADO_ID,
        AUTH_HEADER,
      );
    });

    it('valida al usuario en auth-usuarios antes de abrir la transaccion', async () => {
      await asignar();

      const ordenValidacion =
        authUsuariosClient.obtenerUsuarioConRoles.mock.invocationCallOrder[0];
      const ordenTransaccion = prisma.$transaction.mock.invocationCallOrder[0];

      expect(ordenValidacion).toBeLessThan(ordenTransaccion);
    });
  });

  describe('reasignacion y trazabilidad', () => {
    const asignacionVigente = {
      id: 'f0eebc99-9c0b-4ef8-bb6d-6bb9bd380a40',
      incidenteId: INCIDENTE_ID,
      usuarioAsignadoId: OTRO_USUARIO_ID,
      asignadoPorId: ACTOR_ID,
      fechaAsignacion: new Date('2026-10-01T10:00:00.000Z'),
      activa: true,
    };

    beforeEach(() => {
      tx.asignacion.findFirst.mockResolvedValue(asignacionVigente);
    });

    it('busca la asignacion activa del incidente', async () => {
      await asignar();

      expect(tx.asignacion.findFirst).toHaveBeenCalledWith({
        where: { incidenteId: INCIDENTE_ID, activa: true },
        orderBy: { fechaAsignacion: 'desc' },
      });
    });

    it('desactiva la asignacion vigente antes de crear la nueva activa', async () => {
      const resultado = await asignar();

      expect(tx.asignacion.update).toHaveBeenCalledTimes(1);
      expect(tx.asignacion.update).toHaveBeenCalledWith({
        where: { id: asignacionVigente.id },
        data: { activa: false },
      });
      expect(tx.asignacion.create).toHaveBeenCalledTimes(1);
      expect(tx.asignacion.create).toHaveBeenCalledWith({
        data: {
          incidenteId: INCIDENTE_ID,
          usuarioAsignadoId: USUARIO_ASIGNADO_ID,
          asignadoPorId: ACTOR_ID,
          activa: true,
        },
      });

      const ordenUpdate = tx.asignacion.update.mock.invocationCallOrder[0];
      const ordenCreate = tx.asignacion.create.mock.invocationCallOrder[0];
      expect(ordenUpdate).toBeLessThan(ordenCreate);

      expect(resultado.usuarioAsignadoId).toBe(USUARIO_ASIGNADO_ID);
      expect(resultado.activa).toBe(true);
    });

    it('registra en el historial quien fue reasignado y a quien', async () => {
      await asignar();

      expect(tx.historialEstado.create).toHaveBeenCalledTimes(1);
      expect(tx.historialEstado.create).toHaveBeenCalledWith({
        data: {
          incidenteId: INCIDENTE_ID,
          estadoId: ESTADO_ID,
          usuarioId: ACTOR_ID,
          observacion: `Reasignado de ${OTRO_USUARIO_ID} a ${USUARIO_ASIGNADO_ID}`,
        },
      });
    });
  });

  describe('idempotencia', () => {
    const asignacionVigente = {
      id: ASIGNACION_ID,
      incidenteId: INCIDENTE_ID,
      usuarioAsignadoId: USUARIO_ASIGNADO_ID,
      asignadoPorId: ACTOR_ID,
      fechaAsignacion: FECHA,
      activa: true,
    };

    it('devuelve la asignacion vigente sin crear, actualizar ni registrar historial', async () => {
      tx.asignacion.findFirst.mockResolvedValue(asignacionVigente);

      const resultado = await asignar();

      expect(resultado).toEqual(asignacionVigente);
      expect(tx.asignacion.update).not.toHaveBeenCalled();
      expect(tx.asignacion.create).not.toHaveBeenCalled();
      expect(tx.historialEstado.create).not.toHaveBeenCalled();
    });
  });

  describe('validacion con auth-usuarios', () => {
    it('responde 400 si el usuario propuesto no tiene rol ADMIN y no abre la transaccion', async () => {
      authUsuariosClient.obtenerUsuarioConRoles.mockResolvedValue({
        id: USUARIO_ASIGNADO_ID,
        nombre: 'Carlos Ciudadano',
        email: 'carlos@example.com',
        roles: ['CIUDADANO'],
      });

      await expect(asignar()).rejects.toBeInstanceOf(BadRequestException);

      expect(prisma.$transaction).not.toHaveBeenCalled();
      expect(tx.asignacion.create).not.toHaveBeenCalled();
      expect(tx.historialEstado.create).not.toHaveBeenCalled();
    });

    it('responde 400 si auth-usuarios no informa roles', async () => {
      authUsuariosClient.obtenerUsuarioConRoles.mockResolvedValue({
        id: USUARIO_ASIGNADO_ID,
        nombre: 'Sin roles',
        email: 'sin-roles@example.com',
      });

      await expect(asignar()).rejects.toBeInstanceOf(BadRequestException);

      expect(prisma.$transaction).not.toHaveBeenCalled();
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

        const error = await asignar().catch((e: unknown) => e);

        expect(error).toBeInstanceOf(HttpException);
        expect((error as HttpException).getStatus()).toBe(status);
        expect(prisma.$transaction).not.toHaveBeenCalled();
        expect(tx.asignacion.update).not.toHaveBeenCalled();
        expect(tx.asignacion.create).not.toHaveBeenCalled();
        expect(tx.historialEstado.create).not.toHaveBeenCalled();
      },
    );
  });

  describe('validacion de identificadores', () => {
    it('responde 400 si el id del incidente no es un UUID valido', async () => {
      await expect(
        asignar({ usuarioAsignadoId: USUARIO_ASIGNADO_ID }, 'no-es-uuid'),
      ).rejects.toBeInstanceOf(BadRequestException);

      expect(authUsuariosClient.obtenerUsuarioConRoles).not.toHaveBeenCalled();
      expect(prisma.$transaction).not.toHaveBeenCalled();
    });

    it('responde 400 si el usuario asignado no es un UUID valido', async () => {
      await expect(
        asignar({ usuarioAsignadoId: 'no-es-uuid' }),
      ).rejects.toBeInstanceOf(BadRequestException);

      expect(authUsuariosClient.obtenerUsuarioConRoles).not.toHaveBeenCalled();
      expect(prisma.$transaction).not.toHaveBeenCalled();
    });

    it('responde 401 si el actor autenticado no es un UUID valido', async () => {
      await expect(
        asignar({ usuarioAsignadoId: USUARIO_ASIGNADO_ID }, INCIDENTE_ID, ''),
      ).rejects.toBeInstanceOf(UnauthorizedException);

      expect(authUsuariosClient.obtenerUsuarioConRoles).not.toHaveBeenCalled();
      expect(prisma.$transaction).not.toHaveBeenCalled();
    });
  });

  describe('incidente inexistente', () => {
    it('responde 404 si el bloqueo no encuentra el incidente y no escribe nada', async () => {
      tx.$queryRaw.mockResolvedValue([]);

      await expect(asignar()).rejects.toBeInstanceOf(NotFoundException);

      expect(tx.asignacion.findFirst).not.toHaveBeenCalled();
      expect(tx.asignacion.update).not.toHaveBeenCalled();
      expect(tx.asignacion.create).not.toHaveBeenCalled();
      expect(tx.historialEstado.create).not.toHaveBeenCalled();
    });

    it('responde 404 si el incidente desaparece tras el bloqueo y no escribe nada', async () => {
      tx.incidente.findUnique.mockResolvedValue(null);

      await expect(asignar()).rejects.toBeInstanceOf(NotFoundException);

      expect(tx.asignacion.create).not.toHaveBeenCalled();
      expect(tx.historialEstado.create).not.toHaveBeenCalled();
    });
  });
});
