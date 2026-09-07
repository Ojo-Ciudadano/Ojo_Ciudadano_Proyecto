import { Test, TestingModule } from '@nestjs/testing';
import { NotificacionesController } from './notificaciones.controller';
import { NotificacionesService } from './notificaciones.service';

describe('NotificacionesController', () => {
  let notificacionesController: NotificacionesController;

  beforeEach(async () => {
    const app: TestingModule = await Test.createTestingModule({
      controllers: [NotificacionesController],
      providers: [NotificacionesService],
    }).compile();

    notificacionesController = app.get<NotificacionesController>(NotificacionesController);
  });

  describe('root', () => {
    it('should return "Hello World!"', () => {
      expect(notificacionesController.getHello()).toBe('Hello World!');
    });
  });
});
