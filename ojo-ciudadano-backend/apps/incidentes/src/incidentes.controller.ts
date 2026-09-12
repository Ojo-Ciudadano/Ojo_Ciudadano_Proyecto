import { Controller, Get } from '@nestjs/common';
import { IncidentesService } from './incidentes.service';

@Controller()
export class IncidentesController {
  constructor(private readonly incidentesService: IncidentesService) {}

  @Get()
  getHello(): string {
    return this.incidentesService.getHello();
  }
}
