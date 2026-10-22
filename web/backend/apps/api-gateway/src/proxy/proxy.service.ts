import { Injectable, Logger, Optional } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import type { Request, Response } from 'express';

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

    const queryString = Object.keys(req.query).length > 0
      ? '?' + new URLSearchParams(req.query as Record<string, string>).toString()
      : '';
    const targetUrl = `${targetBaseUrl.replace(/\/+$/, '')}/${normalizedPath}${queryString}`;

    const headers: Record<string, string> = {};
    for (const [key, value] of Object.entries(req.headers)) {
      if (key.toLowerCase() === 'host' || key.toLowerCase() === 'content-length') {
        continue;
      }
      if (typeof value === 'string') {
        headers[key] = value;
      } else if (Array.isArray(value)) {
        headers[key] = value.join(', ');
      }
    }

    if (!headers['content-type'] && (req.method === 'POST' || req.method === 'PUT' || req.method === 'PATCH')) {
      headers['content-type'] = 'application/json';
    }

    const hasBody = ['POST', 'PUT', 'PATCH'].includes(req.method.toUpperCase()) && req.body && Object.keys(req.body).length > 0;
    const bodyContent = hasBody ? JSON.stringify(req.body) : undefined;

    try {
      const response = await fetch(targetUrl, {
        method: req.method,
        headers,
        body: bodyContent,
      });

      const responseContentType = response.headers.get('content-type') ?? '';
      res.status(response.status);

      if (responseContentType.includes('application/json')) {
        const data = await response.json();
        res.json(data);
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
}
