import { Module } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { JwtModule } from '@nestjs/jwt';

import { NotificacionesController } from './presentation/controllers/notificaciones.controller';
import { NotificacionesService } from './application/services/notificaciones.service';
import { PrismaModule } from './infrastructure/persistence/prisma/prisma.module';
import { JwtAuthGuard } from './infrastructure/security/jwt-auth.guard';

@Module({
  imports: [
    ConfigModule.forRoot({ isGlobal: true }),
    PrismaModule,

    JwtModule.registerAsync({
      imports: [ConfigModule],
      inject: [ConfigService],
      useFactory: (configService: ConfigService) => ({
        secret: configService.getOrThrow<string>('JWT_SECRET'),
      }),
    }),
  ],
  controllers: [NotificacionesController],
  providers: [NotificacionesService, JwtAuthGuard],
})
export class NotificacionesModule {}
