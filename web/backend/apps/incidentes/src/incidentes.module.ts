import { Module } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { JwtModule } from '@nestjs/jwt';
import { HttpModule } from '@nestjs/axios';
import { AuthUsuariosClient } from './infrastructure/clients/auth-usuarios.client';
import { IncidentesController } from './presentation/controllers/incidentes.controller';
import { HealthController } from './presentation/controllers/health.controller';
import { IncidentesService } from './application/services/incidentes.service';
import { CategoriasModule } from './categorias/categorias.module';
import { EvidenciasModule } from './evidencias/evidencias.module';
import { PrismaModule } from './infrastructure/persistence/prisma/prisma.module';
import { JwtAuthGuard } from './infrastructure/security/jwt-auth.guard';
import { RolesGuard } from './infrastructure/security/roles.guard';
import { IncidentesNotificacionesClient } from './infrastructure/clients/incidentes-notificaciones.client';

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
  controllers: [IncidentesController, HealthController],
  providers: [
    IncidentesService,
    IncidentesNotificacionesClient,
    JwtAuthGuard,
    RolesGuard,
    AuthUsuariosClient,
  ],
})
export class IncidentesModule {}
