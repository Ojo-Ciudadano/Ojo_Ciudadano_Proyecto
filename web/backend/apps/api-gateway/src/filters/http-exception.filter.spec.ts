import {
  BadRequestException,
  HttpStatus,
  NotFoundException,
} from '@nestjs/common';
import { ArgumentsHost } from '@nestjs/common';
import { HttpExceptionFilter } from './http-exception.filter';

describe('HttpExceptionFilter', () => {
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
      originalUrl: '/api/v1/test',
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

  it('debe capturar NotFoundException y formatear la respuesta correctamente', () => {
    const exception = new NotFoundException('Recurso no encontrado');

    filter.catch(exception, mockArgumentsHost);

    expect(mockStatus).toHaveBeenCalledWith(HttpStatus.NOT_FOUND);
    expect(mockJson).toHaveBeenCalledWith(
      expect.objectContaining({
        statusCode: HttpStatus.NOT_FOUND,
        path: '/api/v1/test',
        error: 'Not Found',
        message: 'Recurso no encontrado',
      }),
    );
  });

  it('debe capturar BadRequestException con array de validaciones', () => {
    const validationErrors = [
      'El email es requerido',
      'La contraseña debe tener al menos 6 caracteres',
    ];
    const exception = new BadRequestException(validationErrors);

    filter.catch(exception, mockArgumentsHost);

    expect(mockStatus).toHaveBeenCalledWith(HttpStatus.BAD_REQUEST);
    expect(mockJson).toHaveBeenCalledWith(
      expect.objectContaining({
        statusCode: HttpStatus.BAD_REQUEST,
        path: '/api/v1/test',
        error: 'Bad Request',
        message: validationErrors,
      }),
    );
  });

  it('debe capturar excepciones no controladas y responder con status 500', () => {
    const genericError = new Error('Fallo inesperado de conexión');

    filter.catch(genericError, mockArgumentsHost);

    expect(mockStatus).toHaveBeenCalledWith(HttpStatus.INTERNAL_SERVER_ERROR);
    expect(mockJson).toHaveBeenCalledWith(
      expect.objectContaining({
        statusCode: HttpStatus.INTERNAL_SERVER_ERROR,
        path: '/api/v1/test',
        error: 'INTERNAL_SERVER_ERROR',
        message: 'Fallo inesperado de conexión',
      }),
    );
  });
});
