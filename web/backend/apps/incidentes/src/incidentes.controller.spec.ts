import { Reflector } from '@nestjs/core';
import { Test, TestingModule } from '@nestjs/testing';

import { JwtAuthGuard } from './auth/jwt-auth.guard';
import { Role, ROLES_KEY } from './auth/roles.decorator';
import { IncidentesController } from './incidentes.controller';
import { IncidentesService } from './incidentes.service';

describe('IncidentesController', () => {
  let controller: IncidentesController;
  const mockService = {
    findAll: jest.fn().mockResolvedValue([]),
    findOne: jest.fn().mockResolvedValue({ id: '1' }),
    create: jest.fn().mockResolvedValue({ id: '1' }),
    updatePrioridad: jest.fn().mockResolvedValue({ id: '1', nivelPrioridad: 4 }),
  };

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      controllers: [IncidentesController],
      providers: [
        { provide: IncidentesService, useValue: mockService },
      ],
    })
      .overrideGuard(JwtAuthGuard)
      .useValue({
        canActivate: () => true,
      })
      .compile();

    controller = module.get<IncidentesController>(IncidentesController);
    jest.clearAllMocks();
  });

  it('delega findAll al service sin filtro', async () => {
    await controller.findAll();
    expect(mockService.findAll).toHaveBeenCalledWith(undefined);
  });

  it('delega findAll con filtro usuarioReportaId', async () => {
    await controller.findAll('uuid-1');
    expect(mockService.findAll).toHaveBeenCalledWith('uuid-1');
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

  it('delega updatePrioridad con el id y el dto', async () => {
    await controller.updatePrioridad('incidente-1', { nivelPrioridad: 4 });
    expect(mockService.updatePrioridad).toHaveBeenCalledWith('incidente-1', {
      nivelPrioridad: 4,
    });
  });

  it('restringe updatePrioridad al rol Administrador', () => {
    const reflector = new Reflector();

    expect(
      reflector.getAllAndOverride(ROLES_KEY, [
        IncidentesController.prototype.updatePrioridad,
        IncidentesController,
      ]),
    ).toEqual([Role.ADMIN]);
  });
});
