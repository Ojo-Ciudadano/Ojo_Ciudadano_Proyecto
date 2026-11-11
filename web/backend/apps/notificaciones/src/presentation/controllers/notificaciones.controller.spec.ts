import { Test, TestingModule } from '@nestjs/testing';
import { JwtService } from '@nestjs/jwt';
import { NotificacionesController } from './notificaciones.controller';
import { NotificacionesService } from '../../application/services/notificaciones.service';
import { PrismaService } from '../../infrastructure/persistence/prisma/prisma.service';
import type { Request } from 'express';

const USUARIO_ID = 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380b22';
const INCIDENTE_ID = 'c2eebc99-9c0b-4ef8-bb6d-6bb9bd380c33';

describe('NotificacionesController', () => {
  let notificacionesController: NotificacionesController;

  let prismaMock: {
    notificacion: {
      create: jest.Mock;
      findMany: jest.Mock;
    };
  };

  beforeEach(async () => {
    prismaMock = {
      notificacion: {
        create: jest.fn(),
        findMany: jest.fn(),
      },
    };

    const app: TestingModule = await Test.createTestingModule({
      controllers: [NotificacionesController],
      providers: [
        NotificacionesService,
        {
          provide: PrismaService,
          useValue: prismaMock,
        },
        {
          provide: JwtService,
          useValue: {
            verifyAsync: jest.fn().mockResolvedValue({
              sub: USUARIO_ID,
            }),
          },
        },
      ],
    }).compile();

    notificacionesController = app.get<NotificacionesController>(
      NotificacionesController,
    );
  });

  describe('root', () => {
    it('should return "Hello World!"', () => {
      expect(notificacionesController.getHello()).toBe(
        'Hello World!',
      );
    });
  });

  it('reports process health without checking external dependencies', () => {
    expect(notificacionesController.health()).toEqual({ status: 'ok' });
  });


  describe('create (#113)', () => {
    it('persiste y retorna la notificacion creada con fecha y referencia al incidente', async () => {
      const fechaCreacion = new Date('2026-10-30T00:00:00.000Z');

      const dto = {
        usuarioId: USUARIO_ID,
        incidenteId: INCIDENTE_ID,
        mensaje:
          'Tu incidente "Fuga de agua" cambió de estado de "Nuevo" a "En revisión".',
      };

      prismaMock.notificacion.create.mockResolvedValue({
        id: '30000000-0000-4000-8000-000000000001',
        ...dto,
        leido: false,
        fecha: fechaCreacion,
      });

      const resultado = await notificacionesController.create(dto);

      expect(prismaMock.notificacion.create).toHaveBeenCalledTimes(1);

      expect(prismaMock.notificacion.create).toHaveBeenCalledWith({
        data: dto,
      });

      expect(resultado).toMatchObject({
        id: '30000000-0000-4000-8000-000000000001',
        usuarioId: USUARIO_ID,
        incidenteId: INCIDENTE_ID,
        mensaje: dto.mensaje,
        leido: false,
        fecha: fechaCreacion,
      });
    });
  });

  describe('obtenerMisNotificaciones (#114)', () => {
    it('consulta solamente las notificaciones del usuario autenticado', async () => {
      const notificaciones = [
        {
          id: '30000000-0000-4000-8000-000000000002',
          usuarioId: USUARIO_ID,
          incidenteId: INCIDENTE_ID,
          mensaje: 'Tu incidente fue actualizado',
          leido: false,
          fecha: new Date('2026-11-01T12:00:00.000Z'),
        },
        {
          id: '30000000-0000-4000-8000-000000000001',
          usuarioId: USUARIO_ID,
          incidenteId: INCIDENTE_ID,
          mensaje: 'Tu incidente fue registrado',
          leido: true,
          fecha: new Date('2026-10-30T12:00:00.000Z'),
        },
      ];

      prismaMock.notificacion.findMany.mockResolvedValue(
        notificaciones,
      );

      const req = {
        user: {
          sub: USUARIO_ID,
        },
      } as Request & {
        user: {
          sub: string;
        };
      };

      const resultado =
        await notificacionesController.obtenerMisNotificaciones(req);

      expect(prismaMock.notificacion.findMany).toHaveBeenCalledTimes(1);

      expect(prismaMock.notificacion.findMany).toHaveBeenCalledWith({
        where: {
          usuarioId: USUARIO_ID,
        },
        orderBy: [
          { fecha: 'desc' },
          { id: 'desc' },
        ],
      });

      expect(resultado).toEqual(notificaciones);
    });

    it('retorna una lista vacia cuando el usuario no tiene notificaciones', async () => {
      prismaMock.notificacion.findMany.mockResolvedValue([]);

      const req = {
        user: {
          sub: USUARIO_ID,
        },
      } as Request & {
        user: {
          sub: string;
        };
      };

      const resultado =
        await notificacionesController.obtenerMisNotificaciones(req);

      expect(resultado).toEqual([]);

      expect(prismaMock.notificacion.findMany).toHaveBeenCalledWith({
        where: {
          usuarioId: USUARIO_ID,
        },
        orderBy: [
          { fecha: 'desc' },
          { id: 'desc' },
        ],
      });
    });
  });
});
