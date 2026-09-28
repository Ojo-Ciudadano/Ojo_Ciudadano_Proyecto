import { Injectable } from '@nestjs/common';

@Injectable()
export class NotificacionesService {
  getHello(): string {
    return 'Hello World!';
  }
}
