import { NestFactory } from '@nestjs/core';
import { Logger, ValidationPipe } from '@nestjs/common';
import type { NestExpressApplication } from '@nestjs/platform-express';
import { IncidentesModule } from './incidentes.module';
import { HttpExceptionFilter } from './filters/http-exception.filter';

export const BODY_PARSER_LIMIT = '12mb';

async function bootstrap() {
  const logger = new Logger('Incidentes');
  const app = await NestFactory.create<NestExpressApplication>(IncidentesModule);

  // Configurar límite de carga útil de al menos 12 MB para soportar evidencias de hasta 10 MB (#86)
  app.useBodyParser('json', { limit: BODY_PARSER_LIMIT });
  app.useBodyParser('urlencoded', { limit: BODY_PARSER_LIMIT, extended: true });

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
