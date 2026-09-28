import { Module } from '@nestjs/common';
import { JwtModule } from '@nestjs/jwt';
import { AuthUsuariosController } from './auth-usuarios.controller';
import { AuthUsuariosService } from './auth-usuarios.service';
import { AuthController } from './auth/auth.controller';
import { AuthService } from './auth/auth.service';
import { UsuariosModule } from './usuarios/usuarios.module';
import { RolesModule } from './roles/roles.module';
import { PrismaModule } from './prisma/prisma.module';

@Module({
  imports: [
    PrismaModule,
    UsuariosModule,
    RolesModule,
    JwtModule.register({
      global: true,
      secret: process.env.JWT_SECRET || 'clave_secreta_super_segura_ojo_ciudadano',
      signOptions: { expiresIn: '1d' },
    }),
  ],
  controllers: [
    AuthUsuariosController,
    AuthController,
  ],
  providers: [
    AuthUsuariosService,
    AuthService,
  ],
})
export class AuthUsuariosModule {}