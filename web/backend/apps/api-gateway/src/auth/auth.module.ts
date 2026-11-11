import { Module } from '@nestjs/common';
import { HttpModule } from '@nestjs/axios';
import { ConfigModule } from '@nestjs/config';
import { AuthController } from '../presentation/controllers/auth.controller';
import { AuthService } from '../application/services/auth.service';

@Module({
  imports: [
    ConfigModule,
    HttpModule.register({
      timeout: 5000,
      maxRedirects: 0,
    }),
  ],
  controllers: [AuthController],
  providers: [AuthService],
})
export class AuthModule {}
