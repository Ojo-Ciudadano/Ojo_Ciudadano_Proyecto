import { Module } from '@nestjs/common';
import { ApiGatewayController } from './presentation/controllers/api-gateway.controller';
import { ApiGatewayService } from './application/services/api-gateway.service';
import { AuthModule } from './auth/auth.module';
import { ProxyModule } from './proxy/proxy.module';

@Module({
  imports: [AuthModule, ProxyModule],
  controllers: [ApiGatewayController],
  providers: [ApiGatewayService],
})
export class ApiGatewayModule {}
