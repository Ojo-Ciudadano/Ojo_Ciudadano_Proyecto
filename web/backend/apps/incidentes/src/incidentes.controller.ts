
import {
  BadRequestException,
  Body,
  Controller,
  Get,
  Param,
  ParseUUIDPipe,
  Patch,
  Post,
  Query,
  UseGuards,
} from '@nestjs/common';

import { IncidentesService } from './incidentes.service';
import { CreateIncidenteDto } from './dto/create-incidente.dto';
import { UpdatePrioridadDto } from './dto/update-prioridad.dto';
import { JwtAuthGuard } from './auth/jwt-auth.guard';
import { RolesGuard } from './auth/roles.guard';
import { Role, Roles } from './auth/roles.decorator';

@Controller('incidentes')
@UseGuards(JwtAuthGuard)
export class IncidentesController {
  constructor(
    private readonly incidentesService: IncidentesService,
  ) {}

  @Get()
  findAll(@Query('usuarioReportaId') usuarioReportaId?: string) {
    return this.incidentesService.findAll(usuarioReportaId);
  }

  @Get(':id')
  findOne(@Param('id') id: string) {
    return this.incidentesService.findOne(id);
  }

  @Post()
  create(@Body() dto: CreateIncidenteDto) {
    return this.incidentesService.create(dto);
  }

  @Patch(':id/prioridad')
  @UseGuards(RolesGuard)
  @Roles(Role.ADMIN)
  updatePrioridad(
    @Param(
      'id',
      new ParseUUIDPipe({
        exceptionFactory: () =>
          new BadRequestException('El id del incidente debe ser un UUID valido'),
      }),
    )
    id: string,
    @Body() dto: UpdatePrioridadDto,
  ) {
    return this.incidentesService.updatePrioridad(id, dto);
  }
}
