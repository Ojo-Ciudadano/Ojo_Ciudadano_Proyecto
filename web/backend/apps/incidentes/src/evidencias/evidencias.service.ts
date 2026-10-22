import { randomUUID } from 'node:crypto';
import {
  BadGatewayException,
  BadRequestException,
  Injectable,
  InternalServerErrorException,
  Logger,
  NotFoundException,
  PayloadTooLargeException,
} from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { StorageObjectNotFoundError, StorageService } from '../storage/storage.service';

export const MAX_EVIDENCIA_BYTES = 10 * 1024 * 1024; // 10 MB (límite definido en issue 127)

const EXTENSIONES: Record<string, string> = {
  'image/jpeg': 'jpg',
  'image/png': 'png',
  'image/webp': 'webp',
};

export interface GuardarEvidenciaInput {
  incidenteId: string;
  archivo: Buffer;
  tipoMime: string;
}

@Injectable()
export class EvidenciasService {
  private readonly logger = new Logger(EvidenciasService.name);

  constructor(
    private readonly prisma: PrismaService,
    private readonly storage: StorageService,
  ) {}

  async guardar({ incidenteId, archivo, tipoMime }: GuardarEvidenciaInput) {
    const extension = EXTENSIONES[tipoMime];
    if (!extension) {
      throw new BadRequestException('Tipo de archivo no permitido. Use JPEG, PNG o WebP');
    }
    if (!archivo?.length) {
      throw new BadRequestException('El archivo de evidencia está vacío');
    }
    if (archivo.length > MAX_EVIDENCIA_BYTES) {
      throw new PayloadTooLargeException('La evidencia supera el tamaño máximo de 10 MB');
    }

    const incidente = await this.prisma.incidente.findUnique({
      where: { id: incidenteId },
      select: { id: true },
    });
    if (!incidente) throw new NotFoundException(`Incidente ${incidenteId} no encontrado`);

    const id = randomUUID();
    const claveArchivo = `incidentes/${incidenteId}/${id}.${extension}`;

    // 1) Primero el objeto: si falla, no se crea ningún registro
    try {
      await this.storage.upload(claveArchivo, archivo, tipoMime);
    } catch {
      throw new BadGatewayException('No fue posible almacenar el archivo de evidencia');
    }

    // 2) Luego la metadata; si falla se compensa borrando el objeto
    try {
      return await this.prisma.evidencia.create({
        data: { id, incidenteId, claveArchivo, tipoMime, tamanoArchivo: archivo.length },
      });
    } catch (error) {
      this.logger.error(`Falló el registro de metadata de ${claveArchivo}`, (error as Error).stack);
      await this.compensar(claveArchivo);
      throw new InternalServerErrorException('No fue posible registrar la evidencia');
    }
  }

  async obtenerArchivo(id: string) {
    const evidencia = await this.prisma.evidencia.findUnique({ where: { id } });
    if (!evidencia) throw new NotFoundException(`Evidencia ${id} no encontrada`);

    try {
      const { buffer } = await this.storage.download(evidencia.claveArchivo);
      return { evidencia, archivo: buffer, tipoMime: evidencia.tipoMime };
    } catch (error) {
      if (error instanceof StorageObjectNotFoundError) {
        this.logger.error(`Metadata sin objeto en storage: ${evidencia.claveArchivo}`);
        throw new NotFoundException('El archivo de la evidencia no está disponible');
      }
      throw new BadGatewayException('No fue posible recuperar el archivo de evidencia');
    }
  }

  private async compensar(claveArchivo: string): Promise<void> {
    try {
      await this.storage.remove(claveArchivo);
    } catch {
      this.logger.error(`Objeto huérfano en storage, requiere limpieza manual: ${claveArchivo}`);
    }
  }
}
