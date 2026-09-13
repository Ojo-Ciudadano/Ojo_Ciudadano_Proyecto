import { NestFactory } from '@nestjs/core';
import { Logger } from '@nestjs/common';
import { IncidentesModule } from './incidentes.module';
import { HttpExceptionFilter } from './filters/http-exception.filter';

async function bootstrap() {
  const logger = new Logger('Incidentes');
  const app = await NestFactory.create(IncidentesModule);

  // Filtro global para manejo homogéneo de excepciones (MM-04)
  app.useGlobalFilters(new HttpExceptionFilter());

  const port = process.env.PORT ?? process.env.port ?? 3002;
  await app.listen(port);
  logger.log(`Microservicio incidentes corriendo en http://localhost:${port}`);
}
bootstrap();
