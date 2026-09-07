import { Module } from '@nestjs/common';
import { AuthUsuariosController } from './auth-usuarios.controller';
import { AuthUsuariosService } from './auth-usuarios.service';

@Module({
  imports: [],
  controllers: [AuthUsuariosController],
  providers: [AuthUsuariosService],
})
export class AuthUsuariosModule {}
