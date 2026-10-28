import { Test, TestingModule } from '@nestjs/testing';
import { UsuariosController } from './usuarios.controller';
import { UsuariosService } from './usuarios.service';
import type { Request } from 'express';

describe('UsuariosController', () => {
  let controller: UsuariosController;

  const mockUsuariosService = {
    listarUsuarios: jest.fn(),
    obtenerMiPerfil: jest.fn(),
  };

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      controllers: [UsuariosController],
      providers: [
        {
          provide: UsuariosService,
          useValue: mockUsuariosService,
        },
      ],
    }).compile();

    controller = module.get<UsuariosController>(UsuariosController);
    jest.clearAllMocks();
  });

  // Prueba original
  it('should be defined', () => {
    expect(controller).toBeDefined();
  });

  // Pruebas del perfil autenticado (Issue #118)
  describe('obtenerMiPerfil', () => {
    it('debe utilizar el identificador del usuario autenticado', async () => {
      const request = {
        user: {
          userId: 'usuario-1',
        },
      } as Request & { user: { userId: string } };

      const perfil = {
        id: 'usuario-1',
        nombre: 'Juan Pérez',
        email: 'juan@ejemplo.com',
        telefono: null,
        fechaRegistro: new Date('2026-10-01'),
        roles: ['CIUDADANO'],
      };

      mockUsuariosService.obtenerMiPerfil.mockResolvedValueOnce(perfil);

      const resultado = await controller.obtenerMiPerfil(request);

      expect(mockUsuariosService.obtenerMiPerfil).toHaveBeenCalledWith(
        'usuario-1',
      );

      expect(resultado).toEqual(perfil);
    });

    it('debe consultar el perfil correspondiente a cada sesión', async () => {
      const requestAdmin = {
        user: {
          userId: 'admin-1',
        },
      } as Request & { user: { userId: string } };

      const requestCiudadano = {
        user: {
          userId: 'ciudadano-1',
        },
      } as Request & { user: { userId: string } };

      mockUsuariosService.obtenerMiPerfil
        .mockResolvedValueOnce({
          id: 'admin-1',
          roles: ['ADMIN'],
        })
        .mockResolvedValueOnce({
          id: 'ciudadano-1',
          roles: ['CIUDADANO'],
        });

      const perfilAdmin = await controller.obtenerMiPerfil(requestAdmin);
      const perfilCiudadano = await controller.obtenerMiPerfil(requestCiudadano);

      expect(perfilAdmin.id).toBe('admin-1');
      expect(perfilCiudadano.id).toBe('ciudadano-1');

      expect(mockUsuariosService.obtenerMiPerfil).toHaveBeenNthCalledWith(
        1,
        'admin-1',
      );

      expect(mockUsuariosService.obtenerMiPerfil).toHaveBeenNthCalledWith(
        2,
        'ciudadano-1',
      );
    });
  });
});