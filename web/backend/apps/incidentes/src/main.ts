import { NestFactory } from '@nestjs/core';
import { Logger, ValidationPipe } from '@nestjs/common';
import { IncidentesModule } from './incidentes.module';
import { HttpExceptionFilter } from './filters/http-exception.filter';

async function bootstrap() {
  const logger = new Logger('Incidentes');
  const app = await NestFactory.create(IncidentesModule);

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

  const port = process.env.PORT ?? process.env.port ?? 3002;
  await app.listen(port);
  logger.log(`Microservicio incidentes corriendo en http://localhost:${port}`);
}
bootstrap();
