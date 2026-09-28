import { Test, TestingModule } from '@nestjs/testing';
import { CategoriasService } from './categorias.service';
import { PrismaService } from '../prisma/prisma.service';

describe('CategoriasService', () => {
  let service: CategoriasService;
  const mockPrisma = {
    categoria: { findMany: jest.fn().mockResolvedValue([]) },
  };

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        CategoriasService,
        { provide: PrismaService, useValue: mockPrisma },
      ],
    }).compile();

    service = module.get<CategoriasService>(CategoriasService);
    jest.clearAllMocks();
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });

  it('retorna categorias ordenadas por nombre', async () => {
    await service.findAll();
    expect(mockPrisma.categoria.findMany).toHaveBeenCalledWith({
      orderBy: { nombre: 'asc' },
    });
  });
});
