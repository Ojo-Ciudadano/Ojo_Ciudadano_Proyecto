import {
  ExceptionFilter,
  Catch,
  ArgumentsHost,
  HttpException,
  HttpStatus,
  Logger,
} from '@nestjs/common';
import { Request, Response } from 'express';

export interface ErrorResponseFormat {
  statusCode: number;
  timestamp: string;
  path: string;
  error: string;
  message: string | string[];
}

@Catch()
export class HttpExceptionFilter implements ExceptionFilter {
  private readonly logger = new Logger(HttpExceptionFilter.name);

  catch(exception: unknown, host: ArgumentsHost): void {
    const ctx = host.switchToHttp();
    const response = ctx.getResponse<Response>();
    const request = ctx.getRequest<Request>();

    const isHttpException = exception instanceof HttpException;
    const status = isHttpException
      ? exception.getStatus()
      : HttpStatus.INTERNAL_SERVER_ERROR;

    let errorName = HttpStatus[status] || 'Internal Server Error';
    let message: string | string[] = 'Error interno del servidor';

    if (isHttpException) {
      const res = exception.getResponse();
      if (typeof res === 'string') {
        message = res;
      } else if (typeof res === 'object' && res !== null) {
        const body = res as Record<string, unknown>;
        message = (body.message as string | string[]) ?? exception.message;
        if (body.error && typeof body.error === 'string') {
          errorName = body.error;
        }
      }
    } else if (exception instanceof Error) {
      message = exception.message;
    }

    const errorResponse: ErrorResponseFormat = {
      statusCode: status,
      timestamp: new Date().toISOString(),
      path: request?.originalUrl || request?.url || '',
      error: errorName,
      message,
    };

    if (status >= HttpStatus.INTERNAL_SERVER_ERROR) {
      this.logger.error(
        `HTTP ${status} [${request?.method}] ${errorResponse.path}`,
        exception instanceof Error ? exception.stack : undefined,
      );
    } else {
      this.logger.warn(
        `HTTP ${status} [${request?.method}] ${errorResponse.path} - ${JSON.stringify(message)}`,
      );
    }

    response.status(status).json(errorResponse);
  }
}
