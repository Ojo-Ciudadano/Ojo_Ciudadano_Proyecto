import {
  BadRequestException,
  Body,
  Controller,
  Get,
  HttpCode,
  HttpStatus,
  NotFoundException,
  Param,
  Post,
  Query,
  Res,
  StreamableFile,
  UploadedFiles,
  UseInterceptors,
} from '@nestjs/common';
import { AnyFilesInterceptor } from '@nestjs/platform-express';
import type { Response } from 'express';
import {
  EvidenciaPublicaDto,
  EvidenciasService,
  MAX_UPLOAD_PAYLOAD_BYTES,
} from './evidencias.service';

export interface UploadedEvidenceFile {
  fieldname?: string;
  originalname?: string;
  mimetype: string;
  buffer: Buffer;
  size?: number;
}

@Controller()
export class EvidenciasController {
  constructor(private readonly evidenciasService: EvidenciasService) {}

  @Post('incidentes/:incidenteId/evidencias')
  @HttpCode(HttpStatus.CREATED)
  @UseInterceptors(
    AnyFilesInterceptor({
      limits: {
        fileSize: MAX_UPLOAD_PAYLOAD_BYTES,
      },
    }),
  )
  async subirPorIncidente(
    @Param('incidenteId') incidenteId: string,
    @UploadedFiles() archivos?: UploadedEvidenceFile[],
  ): Promise<EvidenciaPublicaDto> {
    const archivo = this.extraerArchivoUnico(archivos);
    return this.evidenciasService.subirEvidencia({
      incidenteId,
      archivo: archivo.buffer,
      tipoMime: archivo.mimetype,
    });
  }

  @Post('evidencias')
  @HttpCode(HttpStatus.CREATED)
  @UseInterceptors(
    AnyFilesInterceptor({
      limits: {
        fileSize: MAX_UPLOAD_PAYLOAD_BYTES,
      },
    }),
  )
  async subirDirecto(
    @Body('incidenteId') bodyIncidenteId?: string,
    @Query('incidenteId') queryIncidenteId?: string,
    @UploadedFiles() archivos?: UploadedEvidenceFile[],
  ): Promise<EvidenciaPublicaDto> {
    const incidenteId = (bodyIncidenteId ?? queryIncidenteId ?? '').trim();
    const archivo = this.extraerArchivoUnico(archivos);
    return this.evidenciasService.subirEvidencia({
      incidenteId,
      archivo: archivo.buffer,
      tipoMime: archivo.mimetype,
    });
  }

  @Get('incidentes/:incidenteId/evidencias')
  async listarPorIncidente(
    @Param('incidenteId') incidenteId: string,
  ): Promise<EvidenciaPublicaDto[]> {
    return this.evidenciasService.listarPorIncidente(incidenteId);
  }

  @Get('evidencias')
  async listarPorQuery(
    @Query('incidenteId') incidenteId?: string,
  ): Promise<EvidenciaPublicaDto[]> {
    return this.evidenciasService.listarPorIncidente(incidenteId ?? '');
  }

  @Get('evidencias/:id')
  async obtenerMetadata(@Param('id') id: string): Promise<EvidenciaPublicaDto> {
    return this.evidenciasService.obtenerMetadata(id);
  }

  @Get(['evidencias/:id/archivo', 'incidentes/:incidenteId/evidencias/:id/archivo'])
  async descargarArchivo(
    @Param('id') id: string,
    @Res({ passthrough: true }) res: Response,
    @Param('incidenteId') incidenteId?: string,
  ): Promise<StreamableFile> {
    const { evidencia, archivo, tipoMime } =
      await this.evidenciasService.obtenerArchivo(id);

    if (incidenteId && evidencia.incidenteId !== incidenteId) {
      throw new NotFoundException(
        `Evidencia ${id} no encontrada para el incidente ${incidenteId}`,
      );
    }

    res.setHeader('Content-Type', tipoMime);
    res.setHeader('Content-Length', archivo.length.toString());
    res.setHeader('Cache-Control', 'private, max-age=3600');

    return new StreamableFile(archivo);
  }

  private extraerArchivoUnico(
    archivos?: UploadedEvidenceFile[],
  ): UploadedEvidenceFile {
    if (!archivos || archivos.length === 0) {
      throw new BadRequestException('Debe adjuntar un archivo de evidencia');
    }
    if (archivos.length > 1) {
      throw new BadRequestException(
        'Solo se permite subir un archivo de evidencia por solicitud',
      );
    }
    const [archivo] = archivos;
    if (!archivo?.buffer) {
      throw new BadRequestException('El archivo de evidencia es inválido');
    }
    return archivo;
  }
}
