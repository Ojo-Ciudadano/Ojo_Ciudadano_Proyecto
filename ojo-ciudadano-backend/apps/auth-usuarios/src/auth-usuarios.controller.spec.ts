import { Test, TestingModule } from '@nestjs/testing';
import { AuthUsuariosController } from './auth-usuarios.controller';
import { AuthUsuariosService } from './auth-usuarios.service';

describe('AuthUsuariosController', () => {
  let authUsuariosController: AuthUsuariosController;

  beforeEach(async () => {
    const app: TestingModule = await Test.createTestingModule({
      controllers: [AuthUsuariosController],
      providers: [AuthUsuariosService],
    }).compile();

    authUsuariosController = app.get<AuthUsuariosController>(AuthUsuariosController);
  });

  describe('root', () => {
    it('should return "Hello World!"', () => {
      expect(authUsuariosController.getHello()).toBe('Hello World!');
    });
  });
});
