import { Controller, Get } from '@nestjs/common';
import { AuthUsuariosService } from './auth-usuarios.service';

@Controller()
export class AuthUsuariosController {
  constructor(private readonly authUsuariosService: AuthUsuariosService) {}

  @Get()
  getHello(): string {
    return this.authUsuariosService.getHello();
  }
}
