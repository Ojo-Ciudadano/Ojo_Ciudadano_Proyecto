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
});