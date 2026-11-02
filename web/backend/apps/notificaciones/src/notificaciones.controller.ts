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

import type { Request } from 'express';

import { NotificacionesService } from './notificaciones.service';
import { CreateNotificacionDto } from './dto/create-notificacion.dto';
import {
  JwtAuthGuard,
  UsuarioAutenticado,
} from './auth/jwt-auth.guard';

@Controller()
export class NotificacionesController {
  constructor(
    private readonly notificacionesService: NotificacionesService,
  ) {}

  @Get()
  getHello(): string {
    return this.notificacionesService.getHello();
  }

  // Issue #114: consultar las notificaciones del usuario autenticado
  @Get('notificaciones')
  @UseGuards(JwtAuthGuard)
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
  create(@Body() dto: CreateNotificacionDto) {
    return this.notificacionesService.create(dto);
  }
}