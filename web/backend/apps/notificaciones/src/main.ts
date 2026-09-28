import { NestFactory } from '@nestjs/core';
import { Logger, ValidationPipe } from '@nestjs/common';
import { NotificacionesModule } from './notificaciones.module';
import { HttpExceptionFilter } from './filters/http-exception.filter';

async function bootstrap() {
  const logger = new Logger('Notificaciones');
  const app = await NestFactory.create(NotificacionesModule);

  // Filtro global para manejo homogeneo de excepciones (MM-04)
  app.useGlobalFilters(new HttpExceptionFilter());

  // Validacion global de DTOs (MM-05)
  app.useGlobalPipes(
    new ValidationPipe({
      whitelist: true,
      forbidNonWhitelisted: true,
      transform: true,
    }),
  );

  const port = process.env.PORT ?? process.env.port ?? 3003;
  await app.listen(port);
  logger.log(`Microservicio notificaciones corriendo en http://localhost:${port}`);
}
bootstrap();
