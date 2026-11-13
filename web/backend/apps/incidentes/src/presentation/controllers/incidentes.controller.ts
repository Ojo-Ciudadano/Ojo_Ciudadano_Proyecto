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
  Req,
  UseGuards,
} from '@nestjs/common';
import {
  ApiBearerAuth,
  ApiOperation,
  ApiParam,
  ApiResponse,
  ApiTags,
} from '@nestjs/swagger';
import type { Request } from 'express';
import { AsignacionResponseDto } from '../../application/dto/asignacion-response.dto';
import { AsignarResponsableDto } from '../../application/dto/asignar-responsable.dto';
import { CreateIncidenteDto } from '../../application/dto/create-incidente.dto';
import { FindIncidentesQueryDto } from '../../application/dto/find-incidentes-query.dto';
import { HistorialEstadoResponseDto } from '../../application/dto/historial-estado-response.dto';
import {
  IncidenteDetalleResponseDto,
  IncidenteResumenResponseDto,
} from '../../application/dto/incidente-detalle-response.dto';
import { UpdateEstadoDto } from '../../application/dto/update-estado.dto';
import { UpdatePrioridadDto } from '../../application/dto/update-prioridad.dto';
import { IncidentesService } from '../../application/services/incidentes.service';
import { JwtAuthGuard } from '../../infrastructure/security/jwt-auth.guard';
import { RolesGuard } from '../../infrastructure/security/roles.guard';
import { Role, Roles } from '../../infrastructure/security/roles.decorator';
import { CurrentUser } from '../../infrastructure/security/current-user.decorator';
import type { UsuarioAutenticado } from '../../infrastructure/security/current-user.decorator';

@ApiTags('Incidentes')
@ApiBearerAuth()
@Controller('incidentes')
@UseGuards(JwtAuthGuard)
export class IncidentesController {
  constructor(private readonly incidentesService: IncidentesService) {}

  @Get()
  @ApiOperation({
    summary: 'Listar incidentes con filtros opcionales',
    description:
      'Retorna los incidentes ordenados por fechaCreacion descendente. Permite filtrar combinadamente (AND) por usuarioReportaId, estadoId, categoriaId y comuna (#119). Incluye la colección de evidencias disponibles (#106). Acceso permitido para roles ADMIN y CIUDADANO.',
  })
  @ApiResponse({
    status: 200,
    description: 'Listado de incidentes obtenido correctamente.',
    type: [IncidenteResumenResponseDto],
  })
  @ApiResponse({
    status: 400,
    description: 'Parámetros de filtro inválidos (UUID mal formado o comuna vacía).',
  })
  @ApiResponse({
    status: 401,
    description: 'Token Bearer JWT ausente, inválido o expirado.',
  })
  @ApiResponse({
    status: 403,
    description: 'El usuario autenticado no posee un rol permitido (ADMIN o CIUDADANO).',
  })
  findAll(@Query() filtros: FindIncidentesQueryDto) {
    return this.incidentesService.findAll(filtros);
  }

  @Get(':id/historial')
  @ApiOperation({
    summary: 'Consultar el historial de estados de un incidente',
    description:
      'Devuelve el historial de cambios de estado y asignaciones de un incidente en orden cronológico ascendente (#120). Acceso permitido para roles ADMIN y CIUDADANO.',
  })
  @ApiParam({
    name: 'id',
    description: 'UUID del incidente',
    example: 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
  })
  @ApiResponse({
    status: 200,
    description: 'Historial del incidente obtenido correctamente.',
    type: [HistorialEstadoResponseDto],
  })
  @ApiResponse({
    status: 400,
    description: 'El id del incidente no es un UUID válido.',
  })
  @ApiResponse({
    status: 401,
    description: 'Token Bearer JWT ausente, inválido o expirado.',
  })
  @ApiResponse({
    status: 403,
    description: 'El usuario autenticado no posee un rol permitido.',
  })
  @ApiResponse({
    status: 404,
    description: 'Incidente no encontrado.',
  })
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
  @ApiOperation({
    summary: 'Obtener el detalle ampliado de un incidente por ID',
    description:
      'Retorna el detalle completo del incidente incluyendo categoría, estado actual, ubicación, evidencias (#106), historial cronológico de estados y asignación activa vigente (#125). Acceso permitido para roles ADMIN y CIUDADANO.',
  })
  @ApiParam({
    name: 'id',
    description: 'UUID del incidente',
    example: 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
  })
  @ApiResponse({
    status: 200,
    description: 'Detalle ampliado del incidente obtenido correctamente.',
    type: IncidenteDetalleResponseDto,
  })
  @ApiResponse({
    status: 400,
    description: 'El id del incidente no es un UUID válido.',
  })
  @ApiResponse({
    status: 401,
    description: 'Token Bearer JWT ausente, inválido o expirado.',
  })
  @ApiResponse({
    status: 403,
    description: 'El usuario autenticado no posee un rol permitido.',
  })
  @ApiResponse({
    status: 404,
    description: 'Incidente no encontrado.',
  })
  findOne(
    @Param(
      'id',
      new ParseUUIDPipe({
        exceptionFactory: () =>
          new BadRequestException('El id del incidente debe ser un UUID valido'),
      }),
    )
    id: string,
  ) {
    return this.incidentesService.findOne(id);
  }

  @Post()
  @ApiOperation({
    summary: 'Crear un nuevo incidente',
    description:
      'Registra un incidente asignando estado inicial "Nuevo", calculando automáticamente nivelPrioridad desde la prioridadBase de la categoría (#124) y creando el registro inicial en historial_estado. Acceso permitido para roles ADMIN y CIUDADANO.',
  })
  @ApiResponse({
    status: 201,
    description: 'Incidente creado exitosamente.',
    type: IncidenteResumenResponseDto,
  })
  @ApiResponse({
    status: 400,
    description: 'Datos de entrada inválidos o categoría inexistente.',
  })
  @ApiResponse({
    status: 401,
    description: 'Token Bearer JWT ausente, inválido o expirado.',
  })
  @ApiResponse({
    status: 403,
    description: 'El usuario autenticado no posee un rol permitido.',
  })
  create(@Body() dto: CreateIncidenteDto) {
    return this.incidentesService.create(dto);
  }

  @Patch(':id/prioridad')
  @UseGuards(RolesGuard)
  @Roles(Role.ADMIN)
  @ApiOperation({
    summary: 'Modificar la prioridad de un incidente (Solo ADMIN)',
    description:
      'Permite a un Administrador recalificar el nivelPrioridad de un incidente en la escala entera de 1 a 5 (#124).',
  })
  @ApiParam({
    name: 'id',
    description: 'UUID del incidente',
    example: 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
  })
  @ApiResponse({
    status: 200,
    description: 'Prioridad del incidente actualizada correctamente.',
    type: IncidenteResumenResponseDto,
  })
  @ApiResponse({
    status: 400,
    description: 'UUID inválido o nivelPrioridad fuera del rango permitido (1 a 5).',
  })
  @ApiResponse({
    status: 401,
    description: 'Token Bearer JWT ausente, inválido o expirado.',
  })
  @ApiResponse({
    status: 403,
    description: 'Acceso restringido exclusivamente al rol ADMIN.',
  })
  @ApiResponse({
    status: 404,
    description: 'Incidente no encontrado.',
  })
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
  @ApiOperation({
    summary: 'Cambiar el estado de un incidente (Solo ADMIN)',
    description:
      'Aplica una transición de estado válida según la matriz de #116 y registra el movimiento en historial_estado en una misma transacción (#117). La identidad del ejecutor se toma del JWT.',
  })
  @ApiParam({
    name: 'id',
    description: 'UUID del incidente',
    example: 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
  })
  @ApiResponse({
    status: 200,
    description: 'Estado actualizado y registrado en el historial correctamente.',
    type: IncidenteResumenResponseDto,
  })
  @ApiResponse({
    status: 400,
    description: 'UUID inválido, estado no canónico u observación vacía.',
  })
  @ApiResponse({
    status: 401,
    description: 'Token Bearer JWT ausente, inválido o expirado.',
  })
  @ApiResponse({
    status: 403,
    description: 'Acceso restringido exclusivamente al rol ADMIN.',
  })
  @ApiResponse({
    status: 404,
    description: 'Incidente o estado destino no encontrado.',
  })
  @ApiResponse({
    status: 409,
    description:
      'Conflicto: transición de estado no permitida por la matriz de transiciones (incluye solicitud al mismo estado actual).',
  })
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
  @ApiOperation({
    summary: 'Asignar o reasignar responsable a un incidente (Solo ADMIN)',
    description:
      'Asigna un usuario con rol ADMIN vigente como responsable activo del incidente, desactivando la asignación anterior si existía y registrando la trazabilidad en historial_estado (#123).',
  })
  @ApiParam({
    name: 'id',
    description: 'UUID del incidente',
    example: 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
  })
  @ApiResponse({
    status: 200,
    description: 'Responsable asignado o confirmado correctamente.',
    type: AsignacionResponseDto,
  })
  @ApiResponse({
    status: 400,
    description:
      'UUID inválido o el usuario propuesto no posee el rol ADMIN vigente.',
  })
  @ApiResponse({
    status: 401,
    description: 'Token Bearer JWT ausente, inválido o expirado.',
  })
  @ApiResponse({
    status: 403,
    description: 'Acceso restringido exclusivamente al rol ADMIN.',
  })
  @ApiResponse({
    status: 404,
    description: 'Incidente o usuario propuesto no encontrado.',
  })
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
