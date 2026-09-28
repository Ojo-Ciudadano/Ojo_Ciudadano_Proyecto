import { HttpException, Injectable, Logger } from '@nestjs/common';
import { HttpService } from '@nestjs/axios';
import { ConfigService } from '@nestjs/config';
import { AxiosError } from 'axios';
import { firstValueFrom } from 'rxjs';
import { RegisterDto } from './dto/register-dto';
import { LoginDto } from './dto/login-dto';

/**
 * NO contiene lógica de negocio de autenticación
 * (eso vive en auth-usuarios, Tareas 3 y 4). Su única responsabilidad
 * es reenviar la petición y devolver la respuesta real del microservicio,
 * o lanzar un HttpException legible si el microservicio falla.
 */
 
@Injectable()
export class AuthService {
  private readonly logger = new Logger(AuthService.name);
  private readonly authServiceUrl: string;

  constructor(
    private readonly httpService: HttpService,
    private readonly configService: ConfigService,
  ) {
    this.authServiceUrl = this.configService.get<string>(
      'AUTH_SERVICE_URL',
      'http://localhost:3001',
    );
  }

  async register(dto: RegisterDto) {
    return this.forward('POST', '/auth/register', dto);
  }

  async login(dto: LoginDto) {
    return this.forward('POST', '/auth/login', dto);
  }

  private async forward(method: 'POST', path: string, body: unknown) {
    const url = `${this.authServiceUrl}${path}`;
    this.logger.log(`Proxied ${method} ${path} -> ${url}`);

    try {
      const response = await firstValueFrom(
        this.httpService.request({ method, url, data: body }),
      );
      return response.data;
    } catch (error) {
      const axiosError = error as AxiosError;

      if (axiosError.response) {
        // auth-usuarios respondió, pero con error (ej. 400 email duplicado)
        // Se propaga tal cual para que el cliente vea el motivo real.
        throw new HttpException(
          axiosError.response.data ?? 'Error en auth-usuarios',
          axiosError.response.status,
        );
      }

      // auth-usuarios no respondió (caído, URL mal configurada, etc.)
      this.logger.error(`No se pudo contactar a auth-usuarios: ${axiosError.message}`);
      throw new HttpException('Auth service no disponible', 502);
    }
  }
}