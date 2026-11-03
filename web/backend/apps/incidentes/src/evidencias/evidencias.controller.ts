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
import {
  ApiBearerAuth,
  ApiBody,
  ApiConsumes,
  ApiOperation,
  ApiParam,
  ApiProduces,
  ApiQuery,
  ApiResponse,
  ApiTags,
} from '@nestjs/swagger';
import type { Response } from 'express';
import { EvidenciaResponseDto } from '../dto/evidencia-response.dto';
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

@ApiTags('Evidencias')
@ApiBearerAuth()
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
  @ApiOperation({
    summary: 'Subir una fotografía de evidencia asociada a un incidente',
    description:
      'Recibe un único archivo de imagen mediante multipart/form-data (image/jpeg, image/png o image/webp) con un tamaño máximo funcional de 10 MB (límite de transporte: 12 MB). Almacena el objeto encapsuladamente en Supabase Storage y retorna su metadata pública con ruta relativa de descarga (#86).',
  })
  @ApiConsumes('multipart/form-data')
  @ApiParam({
    name: 'incidenteId',
    description: 'UUID del incidente al que se asocia la evidencia',
    example: 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
  })
  @ApiBody({
    description:
      'Formulario multipart/form-data con un único archivo fotográfico (JPEG, PNG o WebP, máx. 10 MB).',
    schema: {
      type: 'object',
      required: ['archivo'],
      properties: {
        archivo: {
          type: 'string',
          format: 'binary',
          description: 'Archivo de imagen (image/jpeg, image/png o image/webp, máx. 10 MB)',
        },
      },
    },
  })
  @ApiResponse({
    status: 201,
    description: 'Evidencia almacenada y registrada exitosamente.',
    type: EvidenciaResponseDto,
  })
  @ApiResponse({
    status: 400,
    description:
      'No se adjuntó archivo, se envió más de un archivo, archivo vacío o formato MIME no permitido.',
  })
  @ApiResponse({
    status: 401,
    description: 'Token Bearer JWT ausente, inválido o expirado.',
  })
  @ApiResponse({
    status: 404,
    description: 'El incidente indicado no existe.',
  })
  @ApiResponse({
    status: 413,
    description: 'El archivo supera el límite máximo permitido de 10 MB (o payload > 12 MB).',
  })
  @ApiResponse({
    status: 502,
    description: 'Fallo al almacenar el archivo en el servicio de almacenamiento.',
  })
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
  @ApiOperation({
    summary: 'Subir una evidencia fotográfica indicando incidenteId en body o query',
    description:
      'Ruta alternativa compatible para carga multipart/form-data de una evidencia fotográfica (JPEG, PNG o WebP, máx. 10 MB) indicando incidenteId en el formulario o query string (#86).',
  })
  @ApiConsumes('multipart/form-data')
  @ApiQuery({
    name: 'incidenteId',
    required: false,
    description: 'UUID del incidente (si no se envía en el body multipart)',
    example: 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
  })
  @ApiBody({
    schema: {
      type: 'object',
      required: ['archivo'],
      properties: {
        incidenteId: {
          type: 'string',
          format: 'uuid',
          example: 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
        },
        archivo: {
          type: 'string',
          format: 'binary',
          description: 'Archivo de imagen (image/jpeg, image/png o image/webp, máx. 10 MB)',
        },
      },
    },
  })
  @ApiResponse({
    status: 201,
    description: 'Evidencia almacenada y registrada exitosamente.',
    type: EvidenciaResponseDto,
  })
  @ApiResponse({
    status: 400,
    description: 'incidenteId ausente, archivo faltante/múltiple o tipo MIME inválido.',
  })
  @ApiResponse({
    status: 401,
    description: 'Token Bearer JWT ausente, inválido o expirado.',
  })
  @ApiResponse({
    status: 404,
    description: 'El incidente indicado no existe.',
  })
  @ApiResponse({
    status: 413,
    description: 'La fotografía supera el límite máximo de 10 MB.',
  })
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
  @ApiOperation({
    summary: 'Listar las evidencias asociadas a un incidente',
    description:
      'Devuelve la colección de metadata pública y referencias relativas (/evidencias/:id/archivo) de las evidencias del incidente en orden cronológico ascendente. Retorna [] si el incidente no tiene evidencias (#86).',
  })
  @ApiParam({
    name: 'incidenteId',
    description: 'UUID del incidente',
    example: 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
  })
  @ApiResponse({
    status: 200,
    description: 'Listado de evidencias del incidente.',
    type: [EvidenciaResponseDto],
  })
  @ApiResponse({
    status: 401,
    description: 'Token Bearer JWT ausente, inválido o expirado.',
  })
  @ApiResponse({
    status: 404,
    description: 'El incidente indicado no existe.',
  })
  async listarPorIncidente(
    @Param('incidenteId') incidenteId: string,
  ): Promise<EvidenciaPublicaDto[]> {
    return this.evidenciasService.listarPorIncidente(incidenteId);
  }

  @Get('evidencias')
  @ApiOperation({
    summary: 'Listar evidencias filtrando por incidenteId en query',
    description:
      'Ruta alternativa para consultar las evidencias de un incidente mediante el parámetro de consulta ?incidenteId= (#86).',
  })
  @ApiQuery({
    name: 'incidenteId',
    required: true,
    description: 'UUID del incidente a consultar',
    example: 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
  })
  @ApiResponse({
    status: 200,
    description: 'Listado de evidencias del incidente.',
    type: [EvidenciaResponseDto],
  })
  @ApiResponse({
    status: 400,
    description: 'El parámetro incidenteId es obligatorio.',
  })
  @ApiResponse({
    status: 401,
    description: 'Token Bearer JWT ausente, inválido o expirado.',
  })
  @ApiResponse({
    status: 404,
    description: 'El incidente indicado no existe.',
  })
  async listarPorQuery(
    @Query('incidenteId') incidenteId?: string,
  ): Promise<EvidenciaPublicaDto[]> {
    return this.evidenciasService.listarPorIncidente(incidenteId ?? '');
  }

  @Get('evidencias/:id')
  @ApiOperation({
    summary: 'Obtener la metadata pública de una evidencia por ID',
    description:
      'Retorna los metadatos de la evidencia (id, incidenteId, tipoMime, tamanoArchivo, fechaCreacion y url relativa) sin exponer rutas internas del bucket (#86).',
  })
  @ApiParam({
    name: 'id',
    description: 'UUID de la evidencia',
    example: '8f14e45f-ceea-467a-9a38-7b23d28f9102',
  })
  @ApiResponse({
    status: 200,
    description: 'Metadata de la evidencia obtenida correctamente.',
    type: EvidenciaResponseDto,
  })
  @ApiResponse({
    status: 401,
    description: 'Token Bearer JWT ausente, inválido o expirado.',
  })
  @ApiResponse({
    status: 404,
    description: 'Evidencia no encontrada.',
  })
  async obtenerMetadata(@Param('id') id: string): Promise<EvidenciaPublicaDto> {
    return this.evidenciasService.obtenerMetadata(id);
  }

  @Get(['evidencias/:id/archivo', 'incidentes/:incidenteId/evidencias/:id/archivo'])
  @ApiOperation({
    summary: 'Descargar o visualizar el archivo binario de una evidencia',
    description:
      'Transmite el contenido binario de la fotografía con su Content-Type original (image/jpeg, image/png o image/webp), Content-Length y Cache-Control: private, max-age=3600 (#86).',
  })
  @ApiParam({
    name: 'id',
    description: 'UUID de la evidencia',
    example: '8f14e45f-ceea-467a-9a38-7b23d28f9102',
  })
  @ApiParam({
    name: 'incidenteId',
    required: false,
    description: 'UUID del incidente (cuando se invoca por la ruta anidada)',
    example: 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
  })
  @ApiProduces('image/jpeg', 'image/png', 'image/webp')
  @ApiResponse({
    status: 200,
    description: 'Stream binario de la imagen de evidencia.',
    schema: {
      type: 'string',
      format: 'binary',
    },
  })
  @ApiResponse({
    status: 401,
    description: 'Token Bearer JWT ausente, inválido o expirado.',
  })
  @ApiResponse({
    status: 404,
    description:
      'Evidencia no encontrada, no pertenece al incidente indicado o el objeto no existe en el almacenamiento.',
  })
  @ApiResponse({
    status: 502,
    description: 'No fue posible recuperar el archivo desde el almacenamiento.',
  })
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
