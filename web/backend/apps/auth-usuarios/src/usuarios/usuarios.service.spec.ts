import { NotFoundException } from '@nestjs/common';
import { Test, TestingModule } from '@nestjs/testing';
import { UsuariosService } from './usuarios.service';
import { PrismaService } from '../prisma/prisma.service';
import { Role } from '../auth/decorators/roles.decorator';

describe('UsuariosService', () => {
  let service: UsuariosService;
  let prisma: PrismaService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        UsuariosService,
        {
          provide: PrismaService,
          useValue: {
            usuario: {
              findUnique: jest.fn(),
              create: jest.fn(),
              findMany: jest.fn(),
              count: jest.fn(),
            },
            $transaction: jest.fn(),
            rol: {
              upsert: jest.fn(),
            },
            usuarioRol: {
              create: jest.fn(),
            },
          },
        },
      ],
    }).compile();

    service = module.get<UsuariosService>(UsuariosService);
    prisma = module.get<PrismaService>(PrismaService);

    jest.clearAllMocks();
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });

  // Pruebas del listado de usuarios (Issue #121)
  describe('listarUsuarios', () => {
    it('debe devolver usuarios paginados sin información sensible', async () => {
      const usuariosSimulados = [
        {
          id: '1',
          nombre: 'Administrador',
          email: 'admin@ejemplo.com',
          telefono: null,
          fechaRegistro: new Date('2026-10-01'),
          roles: [
            {
              rol: {
                nombre: 'ADMIN',
              },
            },
          ],
        },
      ];

      jest.spyOn(prisma, '$transaction').mockResolvedValueOnce([
        usuariosSimulados,
        1,
      ] as never);

      const resultado = await service.listarUsuarios({
        page: 1,
        limit: 10,
      });

      expect(resultado.data).toHaveLength(1);
      expect(resultado.data[0].roles).toEqual(['ADMIN']);

      expect(resultado.meta).toEqual({
        page: 1,
        limit: 10,
        total: 1,
        totalPages: 1,
      });

      expect(resultado.data[0]).not.toHaveProperty('passwordHash');
    });

    it('debe aplicar el filtro ADMIN y calcular la paginación', async () => {
      jest.spyOn(prisma, '$transaction').mockResolvedValueOnce([
        [],
        5,
      ] as never);

      const resultado = await service.listarUsuarios({
        page: 2,
        limit: 2,
        rol: Role.ADMIN,
      });

      expect(prisma.usuario.findMany).toHaveBeenCalledWith(
        expect.objectContaining({
          where: {
            roles: {
              some: {
                rol: {
                  nombre: 'ADMIN',
                },
              },
            },
          },
          skip: 2,
          take: 2,
        }),
      );

      expect(prisma.usuario.count).toHaveBeenCalledWith({
        where: {
          roles: {
            some: {
              rol: {
                nombre: 'ADMIN',
              },
            },
          },
        },
      });

      expect(resultado.meta).toEqual({
        page: 2,
        limit: 2,
        total: 5,
        totalPages: 3,
      });
    });

    it('debe aplicar el filtro CIUDADANO', async () => {
      jest.spyOn(prisma, '$transaction').mockResolvedValueOnce([
        [],
        2,
      ] as never);

      await service.listarUsuarios({
        page: 1,
        limit: 10,
        rol: Role.CIUDADANO,
      });

      expect(prisma.usuario.findMany).toHaveBeenCalledWith(
        expect.objectContaining({
          where: {
            roles: {
              some: {
                rol: {
                  nombre: 'CIUDADANO',
                },
              },
            },
          },
        }),
      );

      expect(prisma.usuario.count).toHaveBeenCalledWith({
        where: {
          roles: {
            some: {
              rol: {
                nombre: 'CIUDADANO',
              },
            },
          },
        },
      });
    });

    it('debe devolver una lista vacía cuando no hay resultados', async () => {
      jest.spyOn(prisma, '$transaction').mockResolvedValueOnce([
        [],
        0,
      ] as never);

      const resultado = await service.listarUsuarios({
        page: 1,
        limit: 10,
      });

      expect(resultado.data).toEqual([]);

      expect(resultado.meta).toEqual({
        page: 1,
        limit: 10,
        total: 0,
        totalPages: 0,
      });
    });
  });

  // Pruebas del perfil autenticado (Issue #118)
  describe('obtenerMiPerfil', () => {
    const fechaRegistro = new Date('2026-10-01');

    it('debe devolver el perfil de un CIUDADANO sin información sensible', async () => {
      jest.spyOn(prisma.usuario, 'findUnique').mockResolvedValueOnce({
        id: 'usuario-1',
        nombre: 'Juan Pérez',
        email: 'juan@ejemplo.com',
        telefono: '912345678',
        fechaRegistro,
        passwordHash: 'hash-secreto',
        roles: [
          {
            rol: {
              nombre: 'CIUDADANO',
            },
          },
        ],
      } as never);

      const resultado = await service.obtenerMiPerfil('usuario-1');

      expect(resultado).toEqual({
        id: 'usuario-1',
        nombre: 'Juan Pérez',
        email: 'juan@ejemplo.com',
        telefono: '912345678',
        fechaRegistro,
        roles: ['CIUDADANO'],
      });

      expect(resultado).not.toHaveProperty('passwordHash');

      expect(prisma.usuario.findUnique).toHaveBeenCalledWith(
        expect.objectContaining({
          where: { id: 'usuario-1' },
        }),
      );

      // Verificar que Prisma no solicite el hash de la contraseña
      const consulta = (prisma.usuario.findUnique as jest.Mock).mock.calls[0][0];

      expect(consulta.select.passwordHash).toBeUndefined();
    });

    it('debe devolver el perfil de un ADMIN', async () => {
      jest.spyOn(prisma.usuario, 'findUnique').mockResolvedValueOnce({
        id: 'usuario-admin',
        nombre: 'Administrador',
        email: 'admin@ejemplo.com',
        telefono: null,
        fechaRegistro,
        roles: [
          {
            rol: {
              nombre: 'ADMIN',
            },
          },
        ],
      } as never);

      const resultado = await service.obtenerMiPerfil('usuario-admin');

      expect(resultado.roles).toEqual(['ADMIN']);
      expect(resultado.id).toBe('usuario-admin');
      expect(resultado).not.toHaveProperty('passwordHash');
    });

    it('debe consultar los roles vigentes en la base de datos', async () => {
      jest.spyOn(prisma.usuario, 'findUnique').mockResolvedValueOnce({
        id: 'usuario-1',
        nombre: 'Juan Pérez',
        email: 'juan@ejemplo.com',
        telefono: null,
        fechaRegistro,
        roles: [
          {
            rol: {
              nombre: 'ADMIN',
            },
          },
        ],
      } as never);

      const resultado = await service.obtenerMiPerfil('usuario-1');

      // El rol devuelto corresponde a la consulta actual de Prisma
      expect(resultado.roles).toEqual(['ADMIN']);
    });

    it('debe devolver 404 cuando el usuario no existe', async () => {
      jest.spyOn(prisma.usuario, 'findUnique').mockResolvedValueOnce(null);

      await expect(
        service.obtenerMiPerfil('usuario-inexistente'),
      ).rejects.toThrow(NotFoundException);
    });

    it('debe consultar únicamente el usuario identificado', async () => {
      jest.spyOn(prisma.usuario, 'findUnique').mockResolvedValueOnce({
        id: 'usuario-2',
        nombre: 'María',
        email: 'maria@ejemplo.com',
        telefono: null,
        fechaRegistro,
        roles: [
          {
            rol: {
              nombre: 'CIUDADANO',
            },
          },
        ],
      } as never);

      const resultado = await service.obtenerMiPerfil('usuario-2');

      expect(prisma.usuario.findUnique).toHaveBeenCalledWith(
        expect.objectContaining({
          where: { id: 'usuario-2' },
        }),
      );

      expect(resultado.id).toBe('usuario-2');
      expect(resultado.email).toBe('maria@ejemplo.com');
    });
  });
});