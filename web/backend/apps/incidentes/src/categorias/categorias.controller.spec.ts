import { Test, TestingModule } from '@nestjs/testing';
import { CategoriasController } from './categorias.controller';
import { CategoriasService } from './categorias.service';

describe('CategoriasController', () => {
  let controller: CategoriasController;
  const mockService = {
    findAll: jest.fn().mockResolvedValue([]),
  };

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      controllers: [CategoriasController],
      providers: [{ provide: CategoriasService, useValue: mockService }],
    }).compile();

    controller = module.get<CategoriasController>(CategoriasController);
    jest.clearAllMocks();
  });

  it('should be defined', () => {
    expect(controller).toBeDefined();
  });

  it('delega findAll al service', async () => {
    await controller.findAll();
    expect(mockService.findAll).toHaveBeenCalled();
  });
});
