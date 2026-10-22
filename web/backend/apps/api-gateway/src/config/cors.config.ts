import type { CorsOptions } from '@nestjs/common/interfaces/external/cors-options.interface';
import { ConfigService } from '@nestjs/config';

export const DEFAULT_LOCAL_CORS_ORIGINS = [
  'http://localhost:5173',
  'http://127.0.0.1:5173',
  'http://localhost:4173',
  'http://127.0.0.1:4173',
];

/**
 * Parsea la lista de orígenes permitidos desde la variable de entorno CORS_ORIGINS
 * (separada por comas). Si no se especifica o queda vacía, utiliza la lista explícita
 * de orígenes locales de desarrollo por defecto.
 */
export function parseCorsOrigins(rawOrigins?: string): string[] {
  if (!rawOrigins || rawOrigins.trim().length === 0) {
    return [...DEFAULT_LOCAL_CORS_ORIGINS];
  }

  const parsed = rawOrigins
    .split(',')
    .map((origin) => origin.trim().replace(/\/+$/, ''))
    .filter((origin) => origin.length > 0);

  return parsed.length > 0 ? parsed : [...DEFAULT_LOCAL_CORS_ORIGINS];
}

/**
 * Construye la configuración de CORS para el API Gateway según el entorno activo.
 * - Permite orígenes explícitamente declarados en CORS_ORIGINS.
 * - Permite solicitudes sin cabecera Origin (clientes móviles nativos Android, curl, healthchecks).
 * - Rechaza orígenes Web no autorizados sin conceder cabeceras Access-Control-Allow-Origin.
 * - Soporta solicitudes preflight (OPTIONS) y cabeceras Authorization (Bearer JWT).
 */
export function buildCorsOptions(configService?: ConfigService): CorsOptions {
  const rawOrigins =
    configService?.get<string>('CORS_ORIGINS') ?? process.env.CORS_ORIGINS;
  const allowedOrigins = parseCorsOrigins(rawOrigins);

  return {
    origin: (
      requestOrigin: string | undefined,
      callback: (err: Error | null, allow?: boolean | string) => void,
    ) => {
      // Solicitudes server-to-server, clientes móviles nativos (Android) o herramientas CLI no envían Origin
      if (!requestOrigin) {
        callback(null, true);
        return;
      }

      const normalizedOrigin = requestOrigin.trim().replace(/\/+$/, '');
      if (allowedOrigins.includes(normalizedOrigin)) {
        callback(null, requestOrigin);
        return;
      }

      // Origen no autorizado: no se emite Access-Control-Allow-Origin para que el navegador bloquee la lectura
      callback(null, false);
    },
    methods: ['GET', 'HEAD', 'PUT', 'PATCH', 'POST', 'DELETE', 'OPTIONS'],
    allowedHeaders: [
      'Content-Type',
      'Authorization',
      'Accept',
      'Origin',
      'X-Requested-With',
    ],
    exposedHeaders: ['Content-Length', 'Content-Type'],
    credentials: true,
    optionsSuccessStatus: 204,
  };
}
