
import { Module } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { JwtModule } from '@nestjs/jwt';

import { IncidentesController } from './incidentes.controller';
import { IncidentesService } from './incidentes.service';
import { CategoriasModule } from './categorias/categorias.module';
import { EvidenciasModule } from './evidencias/evidencias.module';
import { PrismaModule } from './prisma/prisma.module';
import { JwtAuthGuard } from './auth/jwt-auth.guard';

@Module({
  imports: [
    ConfigModule.forRoot({ isGlobal: true }),
    JwtModule.registerAsync({
      inject: [ConfigService],
      useFactory: (configService: ConfigService) => ({
        secret: configService.getOrThrow<string>('JWT_SECRET'),
      }),
    }),
    PrismaModule,
    CategoriasModule,
    EvidenciasModule,
  ],
  controllers: [IncidentesController],
  providers: [IncidentesService, JwtAuthGuard],
})
export class IncidentesModule {}
