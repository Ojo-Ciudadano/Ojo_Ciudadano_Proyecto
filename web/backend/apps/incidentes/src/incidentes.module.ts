import { Module } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { JwtModule } from '@nestjs/jwt';
import { HttpModule } from '@nestjs/axios';
import { AuthUsuariosClient } from './auth/auth-usuarios.client';
import { IncidentesController } from './incidentes.controller';
import { IncidentesService } from './incidentes.service';
import { CategoriasModule } from './categorias/categorias.module';
import { EvidenciasModule } from './evidencias/evidencias.module';
import { PrismaModule } from './prisma/prisma.module';
import { JwtAuthGuard } from './auth/jwt-auth.guard';
import { RolesGuard } from './auth/roles.guard';
import { IncidentesNotificacionesClient } from './notificaciones/incidentes-notificaciones.client';

@Module({
  imports: [
    ConfigModule.forRoot({ isGlobal: true }),
    JwtModule.registerAsync({
      inject: [ConfigService],
      useFactory: (configService: ConfigService) => ({
        secret: configService.getOrThrow<string>('JWT_SECRET'),
      }),
    }),
    HttpModule.register({
      timeout: 5000,
      maxRedirects: 0,
    }),
    PrismaModule,
    CategoriasModule,
    EvidenciasModule,
  ],
  controllers: [IncidentesController],
  providers: [
    IncidentesService,
    IncidentesNotificacionesClient,
    JwtAuthGuard,
    RolesGuard,
    AuthUsuariosClient,
  ],
})
export class IncidentesModule {}
