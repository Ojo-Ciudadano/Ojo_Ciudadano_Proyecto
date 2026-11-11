import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config'; 
import { JwtModule } from '@nestjs/jwt';
import { PassportModule } from '@nestjs/passport';
import { AuthUsuariosController } from './presentation/controllers/auth-usuarios.controller';
import { AuthUsuariosService } from './application/services/auth-usuarios.service';
import { AuthController } from './presentation/controllers/auth.controller';
import { AuthService } from './application/services/auth.service';
import { JwtStrategy } from './infrastructure/security/jwt.strategy';
import { UsuariosModule } from './usuarios/usuarios.module';
import { RolesModule } from './roles/roles.module';
import { PrismaModule } from './infrastructure/persistence/prisma/prisma.module';

@Module({
  imports: [
    ConfigModule.forRoot({ isGlobal: true }), // 👈 Agrega esta línea
    PrismaModule,
    UsuariosModule,
    RolesModule,
    PassportModule.register({ defaultStrategy: 'jwt' }),
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
    JwtStrategy,
  ],
})
export class AuthUsuariosModule {}
