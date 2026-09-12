import { Controller, Get } from '@nestjs/common';
import { NotificacionesService } from './notificaciones.service';

@Controller()
export class NotificacionesController {
  constructor(private readonly notificacionesService: NotificacionesService) {}

  @Get()
  getHello(): string {
    return this.notificacionesService.getHello();
  }
}
