import { Test, TestingModule } from '@nestjs/testing';
import { IncidentesController } from './incidentes.controller';
import { IncidentesService } from './incidentes.service';

describe('IncidentesController', () => {
  let incidentesController: IncidentesController;

  beforeEach(async () => {
    const app: TestingModule = await Test.createTestingModule({
      controllers: [IncidentesController],
      providers: [IncidentesService],
    }).compile();

    incidentesController = app.get<IncidentesController>(IncidentesController);
  });

  describe('root', () => {
    it('should return "Hello World!"', () => {
      expect(incidentesController.getHello()).toBe('Hello World!');
    });
  });
});
