import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { JwtModule } from '@nestjs/jwt';
import { PassportModule } from '@nestjs/passport';
import { AuthUsuariosController } from './presentation/controllers/auth-usuarios.controller';
import { AuthController } from './presentation/controllers/auth.controller';
import { UsuariosController } from './presentation/controllers/usuarios.controller';
import { RolesController } from './presentation/controllers/roles.controller';
import { AuthUsuariosService } from './application/services/auth-usuarios.service';
import { AuthService } from './application/services/auth.service';
import { UsuariosService } from './application/services/usuarios.service';
import { RolesService } from './application/services/roles.service';
import { JwtStrategy } from './infrastructure/security/jwt.strategy';
import { RolesGuard } from './infrastructure/security/roles.guard';
import { PrismaModule } from './infrastructure/persistence/prisma/prisma.module';

@Module({
  imports: [
    ConfigModule.forRoot({ isGlobal: true }),
    PrismaModule,
    PassportModule.register({ defaultStrategy: 'jwt' }),
    JwtModule.register({
      global: true,
      secret:
        process.env.JWT_SECRET || 'clave_secreta_super_segura_ojo_ciudadano',
      signOptions: { expiresIn: '1d' },
    }),
  ],
  controllers: [
    AuthUsuariosController,
    AuthController,
    UsuariosController,
    RolesController,
  ],
  providers: [
    AuthUsuariosService,
    AuthService,
    UsuariosService,
    RolesService,
    JwtStrategy,
    RolesGuard,
  ],
})
export class AuthUsuariosModule {}
