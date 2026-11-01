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
import { Req } from '@nestjs/common';
import type { Request } from 'express';
import { AsignarResponsableDto } from './dto/asignar-responsable.dto';
import { IncidentesService } from './incidentes.service';
import { CreateIncidenteDto } from './dto/create-incidente.dto';
import { FindIncidentesQueryDto } from './dto/find-incidentes-query.dto';
import { UpdateEstadoDto } from './dto/update-estado.dto';
import { UpdatePrioridadDto } from './dto/update-prioridad.dto';
import { JwtAuthGuard } from './auth/jwt-auth.guard';
import { RolesGuard } from './auth/roles.guard';
import { Role, Roles } from './auth/roles.decorator';
import { CurrentUser } from './auth/current-user.decorator';
import type { UsuarioAutenticado } from './auth/current-user.decorator';

@Controller('incidentes')
@UseGuards(JwtAuthGuard)
export class IncidentesController {
  constructor(
    private readonly incidentesService: IncidentesService,
  ) {}

  @Get()
  findAll(@Query() filtros: FindIncidentesQueryDto) {
    return this.incidentesService.findAll(filtros);
  }

  @Get(':id/historial')
  findHistorial(
    @Param(
      'id',
      new ParseUUIDPipe({
        exceptionFactory: () =>
          new BadRequestException('El id del incidente debe ser un UUID valido'),
      }),
    )
    id: string,
  ) {
    return this.incidentesService.findHistorial(id);
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

  @Patch(':id/estado')
  @UseGuards(RolesGuard)
  @Roles(Role.ADMIN)
  updateEstado(
    @Param(
      'id',
      new ParseUUIDPipe({
        exceptionFactory: () =>
          new BadRequestException('El id del incidente debe ser un UUID valido'),
      }),
    )
    id: string,
    @Body() dto: UpdateEstadoDto,
    @CurrentUser() usuario?: UsuarioAutenticado,
  ) {
    // La identidad proviene exclusivamente del token verificado por JwtAuthGuard.
    return this.incidentesService.updateEstado(id, dto, usuario?.sub ?? '');
  }

  @Patch(':id/asignacion')
  @UseGuards(RolesGuard)
  @Roles(Role.ADMIN)
  asignarResponsable(
    @Param(
      'id',
      new ParseUUIDPipe({
        exceptionFactory: () =>
          new BadRequestException('El id del incidente debe ser un UUID valido'),
      }),
    )
    id: string,
    @Body() dto: AsignarResponsableDto,
    @CurrentUser() usuario?: UsuarioAutenticado,
    @Req() req?: Request,
  ) {
    const authHeader = req?.headers?.authorization;
    return this.incidentesService.asignarResponsable(
      id,
      dto,
      usuario?.sub ?? '',
      typeof authHeader === 'string' ? authHeader : undefined,
    );
  }
}
