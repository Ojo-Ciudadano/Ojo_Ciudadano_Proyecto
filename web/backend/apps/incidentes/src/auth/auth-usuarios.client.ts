import { HttpService } from '@nestjs/axios';
import { HttpException, Injectable, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { AxiosError } from 'axios';
import { firstValueFrom } from 'rxjs';

export interface UsuarioConRoles {
  id: string;
  nombre: string;
  email: string;
  telefono?: string | null;
  roles: string[];
}

@Injectable()
export class AuthUsuariosClient {
  private readonly logger = new Logger(AuthUsuariosClient.name);
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

  async obtenerUsuarioConRoles(usuarioId: string, authHeader?: string): Promise<UsuarioConRoles> {
    const url = `${this.authServiceUrl}/usuarios/${usuarioId}`;
    this.logger.debug(`Validando usuario ${usuarioId} en auth-usuarios: ${url}`);

    try {
      const response = await firstValueFrom(
        this.httpService.get<UsuarioConRoles>(url, {
          headers: authHeader ? { Authorization: authHeader } : undefined,
        }),
      );
      return response.data;
    } catch (error) {
      const axiosError = error as AxiosError;

      if (axiosError.response) {
        const status = axiosError.response.status;
        const data = axiosError.response.data;
        // Propagar con mensaje legible
        if (typeof data === 'object' && data !== null && 'message' in data) {
          throw new HttpException(data, status);
        }
        if (status === 404) {
          throw new HttpException('Usuario no encontrado', 404);
        }
        throw new HttpException(data ?? 'Error al validar usuario en auth-usuarios', status);
      }

      this.logger.error(`No se pudo contactar a auth-usuarios: ${axiosError.message}`);
      throw new HttpException('Servicio de usuarios no disponible', 503);
    }
  }
}