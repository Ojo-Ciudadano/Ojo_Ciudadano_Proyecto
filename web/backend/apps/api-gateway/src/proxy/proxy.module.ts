import { Module } from '@nestjs/common';
import { RolesGuard } from '../infrastructure/security/roles.guard';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { JwtModule } from '@nestjs/jwt';
import { ProxyController } from '../presentation/controllers/proxy.controller';
import { ProxyService } from '../application/services/proxy.service';
import { JwtAuthGuard } from '../infrastructure/security/jwt-auth.guard';

@Module({
  imports: [
    ConfigModule.forRoot({ isGlobal: true }),
    JwtModule.registerAsync({
      imports: [ConfigModule],
      inject: [ConfigService],
      useFactory: (configService: ConfigService) => ({
        secret: configService.getOrThrow<string>('JWT_SECRET'),
      }),
    }),
  ],
  controllers: [ProxyController],
  providers: [ProxyService, JwtAuthGuard, RolesGuard],
  exports: [ProxyService],
})
export class ProxyModule {}
