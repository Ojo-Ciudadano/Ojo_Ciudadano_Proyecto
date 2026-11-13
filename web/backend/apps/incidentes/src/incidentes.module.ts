import { Module } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { JwtModule } from '@nestjs/jwt';
import { HttpModule } from '@nestjs/axios';
import { AuthUsuariosClient } from './infrastructure/clients/auth-usuarios.client';
import { IncidentesController } from './presentation/controllers/incidentes.controller';
import { CategoriasController } from './presentation/controllers/categorias.controller';
import { EvidenciasController } from './presentation/controllers/evidencias.controller';
import { HealthController } from './presentation/controllers/health.controller';
import { IncidentesService } from './application/services/incidentes.service';
import { CategoriasService } from './application/services/categorias.service';
import { EvidenciasService } from './application/services/evidencias.service';
import { PrismaModule } from './infrastructure/persistence/prisma/prisma.module';
import { StorageModule } from './infrastructure/storage/storage.module';
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
    StorageModule,
  ],
  controllers: [
    IncidentesController,
    CategoriasController,
    EvidenciasController,
    HealthController,
  ],
  providers: [
    IncidentesService,
    CategoriasService,
    EvidenciasService,
    IncidentesNotificacionesClient,
    JwtAuthGuard,
    RolesGuard,
    AuthUsuariosClient,
  ],
})
export class IncidentesModule {}
