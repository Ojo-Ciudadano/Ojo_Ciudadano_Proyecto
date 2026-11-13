import {
  Body,
  Controller,
  Get,
  HttpCode,
  HttpStatus,
  Post,
  Req,
  UseGuards,
} from '@nestjs/common';
import {
  ApiBearerAuth,
  ApiExcludeEndpoint,
  ApiOperation,
  ApiResponse,
  ApiTags,
} from '@nestjs/swagger';

import type { Request } from 'express';

import { NotificacionesService } from '../../application/services/notificaciones.service';
import {
  CreateNotificacionDto,
  NotificacionResponseDto,
} from '../../application/dto/create-notificacion.dto';
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
}
