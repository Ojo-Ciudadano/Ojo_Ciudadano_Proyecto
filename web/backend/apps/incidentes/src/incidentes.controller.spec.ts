import { Test, TestingModule } from '@nestjs/testing';
import { IncidentesController } from './incidentes.controller';
import { IncidentesService } from './incidentes.service';

describe('IncidentesController', () => {
  let controller: IncidentesController;
  const mockService = {
    findAll: jest.fn().mockResolvedValue([]),
    findOne: jest.fn().mockResolvedValue({ id: '1' }),
    create: jest.fn().mockResolvedValue({ id: '1' }),
  };

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      controllers: [IncidentesController],
      providers: [{ provide: IncidentesService, useValue: mockService }],
    }).compile();
    controller = module.get<IncidentesController>(IncidentesController);
    jest.clearAllMocks();
  });

  it('delega findAll al service', async () => {
    await controller.findAll();
    expect(mockService.findAll).toHaveBeenCalled();
  });

  it('delega findOne con el id', async () => {
    await controller.findOne('1');
    expect(mockService.findOne).toHaveBeenCalledWith('1');
  });

  it('delega create con el dto', async () => {
    const dto: any = { titulo: 't', descripcion: 'd', categoriaId: 'c', usuarioReportaId: 'u', ubicacion: {} };
    await controller.create(dto);
    expect(mockService.create).toHaveBeenCalledWith(dto);
  });
});