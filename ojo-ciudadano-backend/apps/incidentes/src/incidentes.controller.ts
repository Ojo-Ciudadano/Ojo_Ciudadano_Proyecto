import { Controller, Post, Body, Get } from '@nestjs/common';
import { IncidentesService } from './incidentes.service';
import { ApiTags, ApiOperation, ApiResponse, ApiBearerAuth } from '@nestjs/swagger';
import { CreateIncidenteDto } from './dto/create-incidente.dto';

@ApiTags('Incidentes')
@ApiBearerAuth() // falta el token JWT, el cual deberia pasarme la fran al terminar su tarea nº4
@Controller('incidentes')
export class IncidentesController {
  constructor(private readonly incidentesService: IncidentesService) {}

  @Post()
  @ApiOperation({ summary: 'Reportar un nuevo incidente' })
  @ApiResponse({ status: 201, description: 'Incidente creado y derivado a revisión.' })
  @ApiResponse({ status: 401, description: 'No autorizado. Token faltante o inválido.' })
  crear(@Body() createIncidenteDto: CreateIncidenteDto) {
    return { mensaje: 'Endpoint documentado' };
  }

  @Get()
  @ApiOperation({ summary: 'Listar todos los incidentes activos' })
  @ApiResponse({ status: 200, description: 'Lista de incidentes retornada exitosamente.' })
  obtenerTodos() {
    return { mensaje: 'Endpoint documentado' };
  }
}