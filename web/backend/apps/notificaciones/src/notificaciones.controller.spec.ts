import { Test, TestingModule } from '@nestjs/testing';
import { NotificacionesController } from './notificaciones.controller';
import { NotificacionesService } from './notificaciones.service';
import { PrismaService } from './prisma/prisma.service';

describe('NotificacionesController', () => {
  let notificacionesController: NotificacionesController;
  let prismaMock: { notificacion: { create: jest.Mock } };

  beforeEach(async () => {
    prismaMock = {
      notificacion: {
        create: jest.fn(),
      },
    };

    const app: TestingModule = await Test.createTestingModule({
      controllers: [NotificacionesController],
      providers: [
        NotificacionesService,
        { provide: PrismaService, useValue: prismaMock },
      ],
    }).compile();

    notificacionesController = app.get<NotificacionesController>(
      NotificacionesController,
    );
  });

  describe('root', () => {
    it('should return "Hello World!"', () => {
      expect(notificacionesController.getHello()).toBe('Hello World!');
    });
  });

  describe('create', () => {
    it('persiste y retorna la notificacion creada con fecha y referencia al incidente', async () => {
      const fechaCreacion = new Date('2026-10-30T00:00:00.000Z');
      const dto = {
        usuarioId: 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380b22',
        incidenteId: 'c2eebc99-9c0b-4ef8-bb6d-6bb9bd380c33',
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
        usuarioId: dto.usuarioId,
        incidenteId: dto.incidenteId,
        mensaje: dto.mensaje,
        leido: false,
        fecha: fechaCreacion,
      });
    });
  });
});
