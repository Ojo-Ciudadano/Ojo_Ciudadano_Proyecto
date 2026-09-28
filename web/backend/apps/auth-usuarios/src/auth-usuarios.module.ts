import { Module } from '@nestjs/common';
import { AuthUsuariosController } from './auth-usuarios.controller';
import { AuthUsuariosService } from './auth-usuarios.service';
import { UsuariosModule } from './usuarios/usuarios.module';
import { RolesModule } from './roles/roles.module';

@Module({
  imports: [UsuariosModule, RolesModule],
  controllers: [AuthUsuariosController],
  providers: [AuthUsuariosService],
})
export class AuthUsuariosModule {}
