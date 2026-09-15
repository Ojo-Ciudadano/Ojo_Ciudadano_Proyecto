import {
  BadRequestException,
  HttpStatus,
  NotFoundException,
} from '@nestjs/common';
import { ArgumentsHost } from '@nestjs/common';
import { HttpExceptionFilter } from './http-exception.filter';

describe('Notificaciones HttpExceptionFilter', () => {
  let filter: HttpExceptionFilter;
  let mockJson: jest.Mock;
  let mockStatus: jest.Mock;
  let mockGetResponse: jest.Mock;
  let mockGetRequest: jest.Mock;
  let mockArgumentsHost: ArgumentsHost;

  beforeEach(() => {
    filter = new HttpExceptionFilter();
    mockJson = jest.fn();
    mockStatus = jest.fn().mockReturnValue({ json: mockJson });
    mockGetResponse = jest.fn().mockReturnValue({
      status: mockStatus,
    });
    mockGetRequest = jest.fn().mockReturnValue({
      originalUrl: '/notificaciones/789',
      method: 'GET',
    });

    mockArgumentsHost = {
      switchToHttp: jest.fn().mockReturnValue({
        getResponse: mockGetResponse,
        getRequest: mockGetRequest,
      }),
    } as unknown as ArgumentsHost;
  });

  it('debe estar definido', () => {
    expect(filter).toBeDefined();
  });

  it('debe capturar NotFoundException y formatear la respuesta', () => {
    const exception = new NotFoundException('Notificación no encontrada');

    filter.catch(exception, mockArgumentsHost);

    expect(mockStatus).toHaveBeenCalledWith(HttpStatus.NOT_FOUND);
    expect(mockJson).toHaveBeenCalledWith(
      expect.objectContaining({
        statusCode: HttpStatus.NOT_FOUND,
        path: '/notificaciones/789',
        error: 'Not Found',
        message: 'Notificación no encontrada',
      }),
    );
  });

  it('debe capturar BadRequestException con array de validaciones', () => {
    const validationErrors = [
      'El canal de notificación es requerido',
      'El destinatario no es válido',
    ];
    const exception = new BadRequestException(validationErrors);

    filter.catch(exception, mockArgumentsHost);

    expect(mockStatus).toHaveBeenCalledWith(HttpStatus.BAD_REQUEST);
    expect(mockJson).toHaveBeenCalledWith(
      expect.objectContaining({
        statusCode: HttpStatus.BAD_REQUEST,
        path: '/notificaciones/789',
        error: 'Bad Request',
        message: validationErrors,
      }),
    );
  });

  it('debe capturar error imprevisto y responder 500', () => {
    const error = new Error('SMTP connection failure');

    filter.catch(error, mockArgumentsHost);

    expect(mockStatus).toHaveBeenCalledWith(HttpStatus.INTERNAL_SERVER_ERROR);
    expect(mockJson).toHaveBeenCalledWith(
      expect.objectContaining({
        statusCode: HttpStatus.INTERNAL_SERVER_ERROR,
        path: '/notificaciones/789',
        error: 'INTERNAL_SERVER_ERROR',
        message: 'SMTP connection failure',
      }),
    );
  });
});
