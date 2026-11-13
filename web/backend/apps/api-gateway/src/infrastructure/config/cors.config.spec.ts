import { ConfigService } from '@nestjs/config';
import {
  buildCorsOptions,
  DEFAULT_LOCAL_CORS_ORIGINS,
  parseCorsOrigins,
} from './cors.config';

describe('cors.config (#127)', () => {
  describe('parseCorsOrigins', () => {
    it('retorna los orígenes locales por defecto cuando no se define CORS_ORIGINS', () => {
      expect(parseCorsOrigins(undefined)).toEqual(DEFAULT_LOCAL_CORS_ORIGINS);
      expect(parseCorsOrigins('   ')).toEqual(DEFAULT_LOCAL_CORS_ORIGINS);
    });

    it('parsea y normaliza una lista de orígenes separados por coma quitando barras finales', () => {
      const raw =
        ' https://ojociudadano.dev.censei.cl/ , http://localhost:5173 ';
      expect(parseCorsOrigins(raw)).toEqual([
        'https://ojociudadano.dev.censei.cl',
        'http://localhost:5173',
      ]);
    });
  });

  describe('buildCorsOptions', () => {
    const evaluateOrigin = (
      corsOriginsEnv: string | undefined,
      requestOrigin: string | undefined,
    ): Promise<{ err: Error | null; allow?: boolean | string }> => {
      const configService = {
        get: jest.fn((key: string) =>
          key === 'CORS_ORIGINS' ? corsOriginsEnv : undefined,
        ),
      } as unknown as ConfigService;

      const options = buildCorsOptions(configService);
      const originFn = options.origin as (
        origin: string | undefined,
        cb: (err: Error | null, allow?: boolean | string) => void,
      ) => void;

      return new Promise((resolve) => {
        originFn(requestOrigin, (err, allow) => resolve({ err, allow }));
      });
    };

    it('permite solicitudes sin cabecera Origin (Android, curl, healthchecks)', async () => {
      const result = await evaluateOrigin(
        'https://ojociudadano.dev.censei.cl',
        undefined,
      );
      expect(result.err).toBeNull();
      expect(result.allow).toBe(true);
    });

    it('permite un origen explícitamente configurado en CORS_ORIGINS', async () => {
      const result = await evaluateOrigin(
        'https://ojociudadano.dev.censei.cl,http://localhost:5173',
        'https://ojociudadano.dev.censei.cl',
      );
      expect(result.err).toBeNull();
      expect(result.allow).toBe('https://ojociudadano.dev.censei.cl');
    });

    it('rechaza un origen no autorizado devolviendo allow=false sin lanzar excepción 500', async () => {
      const result = await evaluateOrigin(
        'https://ojociudadano.dev.censei.cl',
        'https://sitio-malicioso.cl',
      );
      expect(result.err).toBeNull();
      expect(result.allow).toBe(false);
    });

    it('incluye soporte para Authorization y métodos del Sprint 2 en preflight', () => {
      const options = buildCorsOptions();
      expect(options.allowedHeaders).toContain('Authorization');
      expect(options.allowedHeaders).toContain('Content-Type');
      expect(options.methods).toEqual(
        expect.arrayContaining([
          'GET',
          'POST',
          'PUT',
          'PATCH',
          'DELETE',
          'OPTIONS',
        ]),
      );
      expect(options.optionsSuccessStatus).toBe(204);
    });
  });
});
