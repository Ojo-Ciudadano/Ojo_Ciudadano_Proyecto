import { All, Controller, Get, Patch, Req, Res, UseGuards } from '@nestjs/common';
import { RolesGuard } from './roles.guard';
import { Role, Roles } from './roles.decorator';
import type { Request, Response } from 'express';
import { JwtAuthGuard } from './jwt-auth.guard';
import { ProxyService } from './proxy.service';

@Controller()
export class ProxyController {
  constructor(private readonly proxyService: ProxyService) {}

  @All(['auth', 'auth/*', 'usuarios', 'usuarios/*', 'roles', 'roles/*'])
  async handleAuthProxy(@Req() req: Request, @Res() res: Response): Promise<void> {
    await this.proxyService.forward(req, res);
  }

  // Debe declararse antes del catch-all de incidentes para que el cambio de
  // estado quede restringido al Administrador (Issue #117).
  @Patch('incidentes/:id/estado')
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles(Role.ADMIN)
  async handleIncidentesEstadoProxy(
    @Req() req: Request,
    @Res() res: Response,
  ): Promise<void> {
    await this.proxyService.forward(req, res);
  }
  // Debe declararse antes del catch-all de incidentes para que la modificacion de
  // prioridad quede restringida al Administrador (Issue #124).
  @Patch('incidentes/:id/prioridad')
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles(Role.ADMIN)
  async handleIncidentesPrioridadProxy(
    @Req() req: Request,
    @Res() res: Response,
  ): Promise<void> {
    await this.proxyService.forward(req, res);
  }
  // Asignación/reasignación de responsables - solo Administrador (Issue #123)
  @Patch('incidentes/:id/asignacion')
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles(Role.ADMIN)
  async handleIncidentesAsignacionProxy(
    @Req() req: Request,
    @Res() res: Response,
  ): Promise<void> {
    await this.proxyService.forward(req, res);
  }

  @All(['incidentes', 'incidentes/*', 'categorias', 'categorias/*', 'evidencias', 'evidencias/*'])
  @UseGuards(JwtAuthGuard, RolesGuard)
  async handleIncidentesProxy(@Req() req: Request, @Res() res: Response): Promise<void> {
    await this.proxyService.forward(req, res);
  }

  // Solo se expone GET hacia el exterior con autenticacion JWT; la creacion
  // (POST /notificaciones) queda reservada para comunicacion interna entre microservicios (#113).
  @Get(['notificaciones', 'notificaciones/*'])
  @UseGuards(JwtAuthGuard, RolesGuard)
  async handleNotificacionesProxy(@Req() req: Request, @Res() res: Response): Promise<void> {
    await this.proxyService.forward(req, res);
  }
}
