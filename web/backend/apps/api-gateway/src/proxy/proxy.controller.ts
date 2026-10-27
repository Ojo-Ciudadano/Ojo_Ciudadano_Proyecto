import { All, Controller, Patch, Req, Res, UseGuards } from '@nestjs/common';
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

  @All(['incidentes', 'incidentes/*', 'categorias', 'categorias/*', 'evidencias', 'evidencias/*'])
  @UseGuards(JwtAuthGuard, RolesGuard)
  async handleIncidentesProxy(@Req() req: Request, @Res() res: Response): Promise<void> {
    await this.proxyService.forward(req, res);
  }

  @All(['notificaciones', 'notificaciones/*'])
  async handleNotificacionesProxy(@Req() req: Request, @Res() res: Response): Promise<void> {
    await this.proxyService.forward(req, res);
  }
}
