import { All, Controller, Get, Patch, Req, Res, UseGuards } from '@nestjs/common';
import { ApiExcludeController } from '@nestjs/swagger';
import { RolesGuard } from './roles.guard';
import { Role, Roles } from './roles.decorator';
import type { Request, Response } from 'express';
import { JwtAuthGuard } from './jwt-auth.guard';
import { ProxyService } from './proxy.service';

@ApiExcludeController()
@Controller()
export class ProxyController {
  constructor(private readonly proxyService: ProxyService) {}

  // Issue #122: cambio de rol de usuarios - solo Administrador
  @Patch('usuarios/:id/rol')
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles(Role.ADMIN)
  async handleUsuarioRolProxy(
    @Req() req: Request,
    @Res() res: Response,
  ): Promise<void> {
    await this.proxyService.forward(req, res);
  }

  // Issue #117: cambio de estado de incidentes - solo Administrador
  @Patch('incidentes/:id/estado')
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles(Role.ADMIN)
  async handleIncidentesEstadoProxy(
    @Req() req: Request,
    @Res() res: Response,
  ): Promise<void> {
    await this.proxyService.forward(req, res);
  }

  // Issue #124: cambio de prioridad de incidentes - solo Administrador
  @Patch('incidentes/:id/prioridad')
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles(Role.ADMIN)
  async handleIncidentesPrioridadProxy(
    @Req() req: Request,
    @Res() res: Response,
  ): Promise<void> {
    await this.proxyService.forward(req, res);
  }

  // Issue #123: asignación y reasignación de responsables - solo Administrador
  @Patch('incidentes/:id/asignacion')
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles(Role.ADMIN)
  async handleIncidentesAsignacionProxy(
    @Req() req: Request,
    @Res() res: Response,
  ): Promise<void> {
    await this.proxyService.forward(req, res);
  }

  // Issue #114: consultar notificaciones del usuario autenticado
  @Get('notificaciones')
  @UseGuards(JwtAuthGuard)
  async handleMisNotificacionesProxy(
    @Req() req: Request,
    @Res() res: Response,
  ): Promise<void> {
    await this.proxyService.forward(req, res);
  }

  // Rutas generales de autenticación y usuarios
  @All(['auth', 'auth/*', 'usuarios', 'usuarios/*', 'roles', 'roles/*'])
  async handleAuthProxy(
    @Req() req: Request,
    @Res() res: Response,
  ): Promise<void> {
    await this.proxyService.forward(req, res);
  }

  // Rutas generales de incidentes
  @All([
    'incidentes',
    'incidentes/*',
    'categorias',
    'categorias/*',
    'evidencias',
    'evidencias/*',
  ])
  @UseGuards(JwtAuthGuard, RolesGuard)
  async handleIncidentesProxy(
    @Req() req: Request,
    @Res() res: Response,
  ): Promise<void> {
    await this.proxyService.forward(req, res);
  }

  // Rutas generales de notificaciones
  @All(['notificaciones', 'notificaciones/*'])
  async handleNotificacionesProxy(
    @Req() req: Request,
    @Res() res: Response,
  ): Promise<void> {
    await this.proxyService.forward(req, res);
  }
}
