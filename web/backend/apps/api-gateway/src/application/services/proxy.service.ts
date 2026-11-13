import { Injectable, Logger, Optional } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import type { Request, Response } from 'express';

export const MAX_PROXY_PAYLOAD_BYTES = 12 * 1024 * 1024; // 12 MB

class PayloadLimitExceededError extends Error {
  constructor() {
    super('El tamaño de la carga útil supera el límite permitido de 12 MB');
    this.name = 'PayloadLimitExceededError';
  }
}

@Injectable()
export class ProxyService {
  private readonly logger = new Logger(ProxyService.name);

  constructor(@Optional() private readonly configService?: ConfigService) {}

  getServiceUrl(prefix: string): string | null {
    switch (prefix) {
      case 'auth':
      case 'usuarios':
      case 'roles':
        return (
          this.configService?.get<string>('AUTH_SERVICE_URL') ??
          process.env.AUTH_SERVICE_URL ??
          'http://localhost:3001'
        );
      case 'incidentes':
      case 'categorias':
      case 'evidencias':
        return (
          this.configService?.get<string>('INCIDENTES_SERVICE_URL') ??
          process.env.INCIDENTES_SERVICE_URL ??
          'http://localhost:3002'
        );
      case 'notificaciones':
        return (
          this.configService?.get<string>('NOTIFICACIONES_SERVICE_URL') ??
          process.env.NOTIFICACIONES_SERVICE_URL ??
          'http://localhost:3003'
        );
      default:
        return null;
    }
  }

  async forward(req: Request, res: Response): Promise<void> {
    const rawPath = req.path; // e.g. /api/v1/incidentes/123 o /incidentes/123
    const normalizedPath = rawPath.replace(/^\/?api\/v1\/?/, ''); // e.g. incidentes/123
    const prefix = normalizedPath.split('/')[0];

    const targetBaseUrl = this.getServiceUrl(prefix);
    if (!targetBaseUrl) {
      res.status(404).json({
        statusCode: 404,
        timestamp: new Date().toISOString(),
        path: rawPath,
        error: 'Not Found',
        message: `Servicio no configurado para la ruta: ${prefix}`,
      });
      return;
    }

    const queryString = Object.keys(req.query ?? {}).length > 0
      ? '?' + new URLSearchParams(req.query as Record<string, string>).toString()
      : '';
    const targetUrl = `${targetBaseUrl.replace(/\/+$/, '')}/${normalizedPath}${queryString}`;

    const headers: Record<string, string> = {};
    for (const [key, value] of Object.entries(req.headers ?? {})) {
      if (key.toLowerCase() === 'host' || key.toLowerCase() === 'content-length') {
        continue;
      }
      if (typeof value === 'string') {
        headers[key] = value;
      } else if (Array.isArray(value)) {
        headers[key] = value.join(', ');
      }
    }

    const method = (req.method ?? 'GET').toUpperCase();
    const isMutation = ['POST', 'PUT', 'PATCH'].includes(method);
    const requestContentType = (headers['content-type'] ?? '').toLowerCase();
    const isBinaryOrMultipart =
      requestContentType.includes('multipart/form-data') ||
      requestContentType.includes('application/octet-stream') ||
      requestContentType.startsWith('image/');

    let bodyContent: BodyInit | undefined;

    if (isMutation) {
      if (Buffer.isBuffer(req.body)) {
        if (req.body.length > MAX_PROXY_PAYLOAD_BYTES) {
          this.sendPayloadTooLarge(res, rawPath);
          return;
        }
        bodyContent = new Uint8Array(req.body);
      } else if (isBinaryOrMultipart) {
        try {
          const rawBuffer = await this.readRawRequestStream(req);
          bodyContent = new Uint8Array(rawBuffer);
        } catch (err) {
          if (err instanceof PayloadLimitExceededError) {
            this.sendPayloadTooLarge(res, rawPath);
            return;
          }
          throw err;
        }
      } else {
        if (!headers['content-type']) {
          headers['content-type'] = 'application/json';
        }
        if (req.body && Object.keys(req.body).length > 0) {
          bodyContent = JSON.stringify(req.body);
        }
      }
    }

    try {
      const response = await fetch(targetUrl, {
        method,
        headers,
        body: bodyContent,
      });

      const responseContentType = response.headers.get('content-type') ?? '';
      res.status(response.status);

      if (responseContentType.includes('application/json')) {
        const data = await response.json();
        res.json(data);
      } else if (
        responseContentType.startsWith('image/') ||
        responseContentType.includes('application/octet-stream')
      ) {
        if (typeof res.setHeader === 'function') {
          res.setHeader('Content-Type', responseContentType);
          const contentLength = response.headers.get('content-length');
          if (contentLength) {
            res.setHeader('Content-Length', contentLength);
          }
          const cacheControl = response.headers.get('cache-control');
          if (cacheControl) {
            res.setHeader('Cache-Control', cacheControl);
          }
        }
        const buffer = Buffer.from(await response.arrayBuffer());
        res.send(buffer);
      } else {
        const text = await response.text();
        res.send(text);
      }
    } catch (error: any) {
      this.logger.error(`Error reenviando solicitud a ${targetUrl}: ${error.message}`);
      res.status(503).json({
        statusCode: 503,
        timestamp: new Date().toISOString(),
        path: rawPath,
        error: 'Service Unavailable',
        message: `Microservicio ${prefix} no disponible en ${targetBaseUrl}`,
      });
    }
  }

  private async readRawRequestStream(req: Request): Promise<Buffer> {
    if (typeof (req as any)?.[Symbol.asyncIterator] !== 'function') {
      return Buffer.alloc(0);
    }

    const chunks: Buffer[] = [];
    let totalBytes = 0;

    for await (const chunk of req as unknown as AsyncIterable<Buffer | string>) {
      const buf = Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk);
      totalBytes += buf.length;
      if (totalBytes > MAX_PROXY_PAYLOAD_BYTES) {
        throw new PayloadLimitExceededError();
      }
      chunks.push(buf);
    }

    return Buffer.concat(chunks, totalBytes);
  }

  private sendPayloadTooLarge(res: Response, path: string): void {
    res.status(413).json({
      statusCode: 413,
      timestamp: new Date().toISOString(),
      path,
      error: 'Payload Too Large',
      message: 'El archivo o carga útil supera el tamaño máximo permitido de 12 MB',
    });
  }
}
