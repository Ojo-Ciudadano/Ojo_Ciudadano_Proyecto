import { Module } from '@nestjs/common';
import { HttpModule } from '@nestjs/axios';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { JwtModule } from '@nestjs/jwt';
import { ApiGatewayController } from './presentation/controllers/api-gateway.controller';
import { AuthController } from './presentation/controllers/auth.controller';
import { ProxyController } from './presentation/controllers/proxy.controller';
import { ApiGatewayService } from './application/services/api-gateway.service';
import { AuthService } from './application/services/auth.service';
import { ProxyService } from './application/services/proxy.service';
import { JwtAuthGuard } from './infrastructure/security/jwt-auth.guard';
import { RolesGuard } from './infrastructure/security/roles.guard';

@Module({
  imports: [
    ConfigModule.forRoot({ isGlobal: true }),
    HttpModule.register({
      timeout: 5000,
      maxRedirects: 0,
    }),
    JwtModule.registerAsync({
      imports: [ConfigModule],
      inject: [ConfigService],
      useFactory: (configService: ConfigService) => ({
        secret: configService.getOrThrow<string>('JWT_SECRET'),
      }),
    }),
  ],
  controllers: [ApiGatewayController, AuthController, ProxyController],
  providers: [
    ApiGatewayService,
    AuthService,
    ProxyService,
    JwtAuthGuard,
    RolesGuard,
  ],
})
export class ApiGatewayModule {}
