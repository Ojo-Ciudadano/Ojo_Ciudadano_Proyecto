import {
  Body,
  Controller,
  Get,
  HttpCode,
  HttpStatus,
  Param,
  Patch,
  Post,
  Req,
  UseGuards,
} from '@nestjs/common';
import {
  ApiBearerAuth,
  ApiExcludeEndpoint,
  ApiOperation,
  ApiParam,
  ApiResponse,
  ApiTags,
} from '@nestjs/swagger';

import type { Request } from 'express';

import { NotificacionesService } from '../../application/services/notificaciones.service';
import {
  CreateNotificacionDto,
  NotificacionResponseDto,
} from '../../application/dto/create-notificacion.dto';
import { MarcarTodasLeidasResponseDto } from '../../application/dto/marcar-todas-leidas-response.dto';
import {
  JwtAuthGuard,
  UsuarioAutenticado,
} from '../../infrastructure/security/jwt-auth.guard';

@ApiTags('Notificaciones')
@Controller()
export class NotificacionesController {
  constructor(
    private readonly notificacionesService: NotificacionesService,
  ) {}

  @Get()
  @ApiExcludeEndpoint()
  getHello(): string {
    return this.notificacionesService.getHello();
  }

  @Get('health')
  @ApiExcludeEndpoint()
  health(): { status: 'ok' } {
    return { status: 'ok' };
  }

  // Issue #114: consultar las notificaciones del usuario autenticado
  @Get('notificaciones')
  @UseGuards(JwtAuthGuard)
  @ApiBearerAuth()
  @ApiOperation({
    summary: 'Consultar las notificaciones del usuario autenticado',
    description:
      'Obtiene la lista de notificaciones pertenecientes exclusivamente al usuario autenticado (extraído del claim sub del token JWT), ordenadas por fecha descendente (#114). Acceso permitido para roles ADMIN y CIUDADANO.',
  })
  @ApiResponse({
    status: 200,
    description: 'Listado de notificaciones del usuario autenticado.',
    type: [NotificacionResponseDto],
  })
  @ApiResponse({
    status: 401,
    description: 'Token Bearer JWT ausente, inválido o expirado.',
  })
  async obtenerMisNotificaciones(
    @Req() req: Request & { user?: UsuarioAutenticado },
  ) {
    return this.notificacionesService.obtenerMisNotificaciones(
      req.user!.sub,
    );
  }

  // Issue #113: crear una notificacion
  @Post('notificaciones')
  @HttpCode(HttpStatus.CREATED)
  @ApiOperation({
    summary: 'Registrar una nueva notificación para un usuario',
    description:
      'Crea y persiste una notificación vinculada a un usuario e incidente específico (#113).',
  })
  @ApiResponse({
    status: 201,
    description: 'Notificación creada exitosamente.',
    type: NotificacionResponseDto,
  })
  @ApiResponse({
    status: 400,
    description: 'UUID de usuario/incidente inválido o mensaje vacío.',
  })
  create(@Body() dto: CreateNotificacionDto) {
    return this.notificacionesService.create(dto);
  }

  @Patch('notificaciones/leidas')
  @UseGuards(JwtAuthGuard)
  @ApiBearerAuth()
  @ApiOperation({
    summary: 'Marcar todas las notificaciones pendientes como leídas',
  })
  @ApiResponse({ status: 200, type: MarcarTodasLeidasResponseDto })
  @ApiResponse({ status: 400, description: 'UUID inválido' })
  @ApiResponse({ status: 401, description: 'Token ausente o inválido' })
  marcarTodas(@Req() req: Request & { user?: UsuarioAutenticado }) {
    return this.notificacionesService.marcarTodasLeidas(req.user!.sub);
  }

  @Patch('notificaciones/:id/leida')
  @UseGuards(JwtAuthGuard)
  @ApiBearerAuth()
  @ApiOperation({ summary: 'Marcar como leída una notificación propia' })
  @ApiParam({ name: 'id', format: 'uuid' })
  @ApiResponse({ status: 200, type: NotificacionResponseDto })
  @ApiResponse({ status: 400, description: 'UUID inválido' })
  @ApiResponse({ status: 401, description: 'Token ausente o inválido' })
  @ApiResponse({
    status: 403,
    description: 'La notificación no pertenece al usuario',
  })
  marcarLeida(
    @Param('id') id: string,
    @Req() req: Request & { user?: UsuarioAutenticado },
  ) {
    return this.notificacionesService.marcarLeida(id, req.user!.sub);
  }
}
