import { Injectable } from '@nestjs/common';
import { PrismaService } from '../../infrastructure/persistence/prisma/prisma.service';

@Injectable()
export class CategoriasService {
  constructor(private readonly prisma: PrismaService) {}
  findAll() { return this.prisma.categoria.findMany({ orderBy: { nombre: 'asc' } }); }
}