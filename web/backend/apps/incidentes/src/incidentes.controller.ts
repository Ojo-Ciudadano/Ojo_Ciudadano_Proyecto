import { Body, Controller, Get, Param, Post } from '@nestjs/common';
import { IncidentesService } from './incidentes.service';
import { CreateIncidenteDto } from './dto/create-incidente.dto';

@Controller('incidentes')
export class IncidentesController {
  constructor(private readonly incidentesService: IncidentesService) {}
  @Get() findAll() { return this.incidentesService.findAll(); }
  @Get(':id') findOne(@Param('id') id: string) { return this.incidentesService.findOne(id); }
  @Post() create(@Body() dto: CreateIncidenteDto) { return this.incidentesService.create(dto); }
}