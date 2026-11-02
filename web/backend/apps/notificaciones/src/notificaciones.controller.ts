import { Body, Controller, Get, HttpCode, HttpStatus, Post } from '@nestjs/common';
import { NotificacionesService } from './notificaciones.service';
import { CreateNotificacionDto } from './dto/create-notificacion.dto';

@Controller()
export class NotificacionesController {
  constructor(private readonly notificacionesService: NotificacionesService) {}

  @Get()
  getHello(): string {
    return this.notificacionesService.getHello();
  }

  @Post('notificaciones')
  @HttpCode(HttpStatus.CREATED)
  create(@Body() dto: CreateNotificacionDto) {
    return this.notificacionesService.create(dto);
  }
}
