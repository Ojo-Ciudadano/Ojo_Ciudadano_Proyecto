import { Injectable } from '@nestjs/common';

@Injectable()
export class AuthUsuariosService {
  getHello(): string {
    return 'Hello World!';
  }
}
