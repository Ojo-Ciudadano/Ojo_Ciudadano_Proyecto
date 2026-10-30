import { ConflictException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';

import { IncidentesService } from './incidentes.service';
import {
  construirMensajeAsignacion,
  construirMensajeCambioEstado,
  IncidentesNotificacionesClient,
} from './notificaciones/incidentes-notificaciones.client';
import { PrismaService } from './prisma/prisma.service';

const INCIDENTE_ID = 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13';
const USUARIO_REPORTA_ID = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11';
const USUARIO_ADMIN_ID = 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12';
const ESTADO_DESTINO_ID = 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a15';

describe('Generacion y persistencia de notificaciones de incidentes (#113)', () => {
  describe('Constructores de mensajes descriptivos', () => {
    it('incluye titulo, estado anterior, estado nuevo y observacion en cambio de estado', () => {
      const mensaje = construirMensajeCambioEstado({
        incidenteId: INCIDENTE_ID,
        titulo: 'Fuga de agua matriz',
        usuarioReportaId: USUARIO_REPORTA_ID,
        estadoAnterior: 'Nuevo',
        estadoNuevo: 'En revisión',
        observacion: 'Se deriva a cuadrilla municipal',
      });

      expect(mensaje).toBe(
        'Tu incidente "Fuga de agua matriz" cambió de estado de "Nuevo" a "En revisión". Observación: Se deriva a cuadrilla municipal',
      );
    });

    it('construye mensaje comprensible de asignacion y reasignacion', () => {
      expect(
        construirMensajeAsignacion({
          incidenteId: INCIDENTE_ID,
          titulo: 'Semáforo apagado',
          usuarioAsignadoId: USUARIO_ADMIN_ID,
          esReasignacion: false,
        }),
      ).toBe(
        'Se te ha asignado como responsable de el incidente "Semáforo apagado".',
      );

      expect(
        construirMensajeAsignacion({
          incidenteId: INCIDENTE_ID,
          titulo: 'Semáforo apagado',
          usuarioAsignadoId: USUARIO_ADMIN_ID,
          esReasignacion: true,
        }),
      ).toBe(
        'Se te ha reasignado como responsable de el incidente "Semáforo apagado".',
      );
    });
  });

  describe('IncidentesNotificacionesClient', () => {
    const fetchOriginal = global.fetch;
    let fetchMock: jest.Mock;

    beforeEach(() => {
      fetchMock = jest.fn();
      global.fetch = fetchMock as unknown as typeof fetch;
    });

    afterEach(() => {
      global.fetch = fetchOriginal;
    });

    it('envia POST /notificaciones al microservicio de Notificaciones con usuarioId, incidenteId y mensaje', async () => {
      fetchMock.mockResolvedValue({ ok: true, status: 201 });
      const config = {
        get: jest.fn().mockReturnValue('http://notificaciones-svc:3003/'),
      } as unknown as ConfigService;
      const client = new IncidentesNotificacionesClient(config);

      const enviado = await client.notificarCambioEstado({
        incidenteId: INCIDENTE_ID,
        titulo: 'Bache profundo',
        usuarioReportaId: USUARIO_REPORTA_ID,
        estadoAnterior: 'Nuevo',
        estadoNuevo: 'En revisión',
        observacion: 'Inspección programada',
      });

      expect(enviado).toBe(true);
      expect(fetchMock).toHaveBeenCalledTimes(1);
      expect(fetchMock).toHaveBeenCalledWith(
        'http://notificaciones-svc:3003/notificaciones',
        {
          method: 'POST',
          headers: { 'content-type': 'application/json' },
          body: JSON.stringify({
            usuarioId: USUARIO_REPORTA_ID,
            incidenteId: INCIDENTE_ID,
            mensaje:
              'Tu incidente "Bache profundo" cambió de estado de "Nuevo" a "En revisión". Observación: Inspección programada',
          }),
        },
      );
    });

    it('gestiona de forma controlada un error HTTP o de red sin lanzar excepcion', async () => {
      fetchMock.mockRejectedValue(new Error('ECONNREFUSED'));
      const client = new IncidentesNotificacionesClient();

      await expect(
        client.notificarCambioEstado({
          incidenteId: INCIDENTE_ID,
          usuarioReportaId: USUARIO_REPORTA_ID,
          estadoNuevo: 'En revisión',
        }),
      ).resolves.toBe(false);
    });
  });

  describe('Integracion con IncidentesService.updateEstado', () => {
    let tx: {
      $queryRaw: jest.Mock;
      incidente: { findUnique: jest.Mock; update: jest.Mock };
      estado: { findUnique: jest.Mock };
      historialEstado: { create: jest.Mock };
    };
    let prisma: { $transaction: jest.Mock };
    let notificacionesClient: {
      notificarCambioEstado: jest.Mock;
    };
    let service: IncidentesService;

    beforeEach(() => {
      tx = {
        $queryRaw: jest.fn().mockResolvedValue([{ id: INCIDENTE_ID }]),
        incidente: {
          findUnique: jest.fn().mockResolvedValue({
            id: INCIDENTE_ID,
            titulo: 'Luminaria dañada',
            usuarioReportaId: USUARIO_REPORTA_ID,
            estado: { nombre: 'Nuevo' },
          }),
          update: jest.fn().mockResolvedValue({
            id: INCIDENTE_ID,
            titulo: 'Luminaria dañada',
            usuarioReportaId: USUARIO_REPORTA_ID,
            estadoId: ESTADO_DESTINO_ID,
            estado: { id: ESTADO_DESTINO_ID, nombre: 'En revisión' },
            evidencias: [],
          }),
        },
        estado: {
          findUnique: jest.fn().mockResolvedValue({
            id: ESTADO_DESTINO_ID,
            nombre: 'En revisión',
          }),
        },
        historialEstado: {
          create: jest.fn().mockResolvedValue({ id: 'hist-1' }),
        },
      };

      prisma = {
        $transaction: jest.fn(async (cb: (tx: unknown) => unknown) => cb(tx)),
      };

      notificacionesClient = {
        notificarCambioEstado: jest.fn().mockResolvedValue(true),
      };

      service = new IncidentesService(
        prisma as unknown as PrismaService,
        notificacionesClient as unknown as IncidentesNotificacionesClient,
      );
    });

    it('genera exactamente una notificacion para el usuario que reporto el incidente cuando cambia el estado', async () => {
      await service.updateEstado(
        INCIDENTE_ID,
        { estado: 'En revisión', observacion: 'En evaluacion tecnica' },
        USUARIO_ADMIN_ID,
      );

      expect(notificacionesClient.notificarCambioEstado).toHaveBeenCalledTimes(1);
      expect(notificacionesClient.notificarCambioEstado).toHaveBeenCalledWith({
        incidenteId: INCIDENTE_ID,
        titulo: 'Luminaria dañada',
        usuarioReportaId: USUARIO_REPORTA_ID,
        estadoAnterior: 'Nuevo',
        estadoNuevo: 'En revisión',
        observacion: 'En evaluacion tecnica',
      });
    });

    it('no genera notificaciones duplicadas ni envia notificacion si la transicion de estado es rechazada', async () => {
      tx.incidente.findUnique.mockResolvedValue({
        id: INCIDENTE_ID,
        titulo: 'Luminaria dañada',
        usuarioReportaId: USUARIO_REPORTA_ID,
        estado: { nombre: 'Nuevo' },
      });
      tx.estado.findUnique.mockResolvedValue({
        id: ESTADO_DESTINO_ID,
        nombre: 'Cerrado',
      });

      await expect(
        service.updateEstado(
          INCIDENTE_ID,
          { estado: 'Cerrado', observacion: 'Cierre directo' },
          USUARIO_ADMIN_ID,
        ),
      ).rejects.toBeInstanceOf(ConflictException);

      expect(notificacionesClient.notificarCambioEstado).not.toHaveBeenCalled();
    });

    it('mantiene exitoso el cambio de estado si el servicio de notificaciones falla', async () => {
      notificacionesClient.notificarCambioEstado.mockResolvedValue(false);

      const resultado = await service.updateEstado(
        INCIDENTE_ID,
        { estado: 'En revisión', observacion: 'Revision inicial' },
        USUARIO_ADMIN_ID,
      );

      expect(resultado.estado.nombre).toBe('En revisión');
      expect(notificacionesClient.notificarCambioEstado).toHaveBeenCalledTimes(1);
    });
  });
});
