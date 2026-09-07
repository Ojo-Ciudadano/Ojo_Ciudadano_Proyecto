import { NestFactory } from '@nestjs/core';
import { NotificacionesModule } from './notificaciones.module';

async function bootstrap() {
  const app = await NestFactory.create(NotificacionesModule);
  await app.listen(process.env.port ?? 3003);
}
bootstrap();
