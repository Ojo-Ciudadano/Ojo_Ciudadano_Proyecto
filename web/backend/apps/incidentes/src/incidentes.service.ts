import { Injectable } from '@nestjs/common';

@Injectable()
export class IncidentesService {
  getHello(): string {
    return 'Hello World!';
  }
}
