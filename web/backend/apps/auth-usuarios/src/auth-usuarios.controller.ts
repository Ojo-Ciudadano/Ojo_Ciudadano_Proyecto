import { Controller, Get } from '@nestjs/common';
import { ApiExcludeEndpoint } from '@nestjs/swagger';
import { AuthUsuariosService } from './auth-usuarios.service';

@Controller()
export class AuthUsuariosController {
  constructor(private readonly authUsuariosService: AuthUsuariosService) {}

  @Get()
  getHello(): string {
    return this.authUsuariosService.getHello();
  }

  @Get('health')
  @ApiExcludeEndpoint()
  health(): { status: 'ok' } {
    return { status: 'ok' };
  }
}
