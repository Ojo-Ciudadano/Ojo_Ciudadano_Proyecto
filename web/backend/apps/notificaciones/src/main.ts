import { NestFactory } from '@nestjs/core';
import { Logger } from '@nestjs/common';
import { NotificacionesModule } from './notificaciones.module';
import { HttpExceptionFilter } from './filters/http-exception.filter';

async function bootstrap() {
  const logger = new Logger('Notificaciones');
  const app = await NestFactory.create(NotificacionesModule);

  // Filtro global para manejo homogéneo de excepciones (MM-04)
  app.useGlobalFilters(new HttpExceptionFilter());

  const port = process.env.PORT ?? process.env.port ?? 3003;
  await app.listen(port);
  logger.log(`Microservicio notificaciones corriendo en http://localhost:${port}`);
}
bootstrap();
