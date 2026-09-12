import { NestFactory } from '@nestjs/core';
import { IncidentesModule } from './incidentes.module';

async function bootstrap() {
  const app = await NestFactory.create(IncidentesModule);
  await app.listen(process.env.port ?? 3002);
}
bootstrap();
