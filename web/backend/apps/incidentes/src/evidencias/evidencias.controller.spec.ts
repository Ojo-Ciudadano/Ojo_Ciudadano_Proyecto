import {
  BadRequestException,
  NotFoundException,
  StreamableFile,
} from '@nestjs/common';
import { Test, TestingModule } from '@nestjs/testing';
import type { Response } from 'express';
import { EvidenciasController } from './evidencias.controller';
import { EvidenciasService } from './evidencias.service';

describe('EvidenciasController', () => {
  let controller: EvidenciasController;
  const mockEvidenciasService = {
    subirEvidencia: jest.fn(),
    listarPorIncidente: jest.fn(),
    obtenerMetadata: jest.fn(),
    obtenerArchivo: jest.fn(),
  };

  const fecha = new Date('2026-10-23T12:00:00.000Z');
  const mockDto = {
    id: 'ev-1',
    incidenteId: 'inc-1',
    tipoMime: 'image/jpeg',
    tamanoArchivo: 4,
    fechaCreacion: fecha,
    url: '/evidencias/ev-1/archivo',
  };

  beforeEach(async () => {
    jest.resetAllMocks();
    const module: TestingModule = await Test.createTestingModule({
      controllers: [EvidenciasController],
      providers: [
        { provide: EvidenciasService, useValue: mockEvidenciasService },
      ],
    }).compile();

    controller = module.get<EvidenciasController>(EvidenciasController);
  });

  it('should be defined', () => {
    expect(controller).toBeDefined();
  });

  describe('subirPorIncidente (POST /incidentes/:incidenteId/evidencias)', () => {
    it('sube el archivo recibido y retorna la evidencia pública', async () => {
      mockEvidenciasService.subirEvidencia.mockResolvedValue(mockDto);
      const buffer = Buffer.from('foto');

      const result = await controller.subirPorIncidente('inc-1', [
        { mimetype: 'image/jpeg', buffer },
      ]);

      expect(mockEvidenciasService.subirEvidencia).toHaveBeenCalledWith({
        incidenteId: 'inc-1',
        archivo: buffer,
        tipoMime: 'image/jpeg',
      });
      expect(result).toEqual(mockDto);
    });

    it('rechaza con 400 si no se adjunta ningún archivo', async () => {
      await expect(controller.subirPorIncidente('inc-1', [])).rejects.toBeInstanceOf(
        BadRequestException,
      );
      expect(mockEvidenciasService.subirEvidencia).not.toHaveBeenCalled();
    });

    it('rechaza con 400 si se adjunta más de un archivo en la misma solicitud', async () => {
      await expect(
        controller.subirPorIncidente('inc-1', [
          { mimetype: 'image/jpeg', buffer: Buffer.from('1') },
          { mimetype: 'image/jpeg', buffer: Buffer.from('2') },
        ]),
      ).rejects.toBeInstanceOf(BadRequestException);
    });
  });

  describe('subirDirecto (POST /evidencias)', () => {
    it('permite subir evidencia indicando incidenteId en body o query', async () => {
      mockEvidenciasService.subirEvidencia.mockResolvedValue(mockDto);
      const buffer = Buffer.from('foto');

      const result = await controller.subirDirecto('inc-1', undefined, [
        { mimetype: 'image/jpeg', buffer },
      ]);

      expect(mockEvidenciasService.subirEvidencia).toHaveBeenCalledWith({
        incidenteId: 'inc-1',
        archivo: buffer,
        tipoMime: 'image/jpeg',
      });
      expect(result).toEqual(mockDto);
    });
  });

  describe('listarPorIncidente / listarPorQuery', () => {
    it('lista las evidencias asociadas a un incidente por parámetro de ruta', async () => {
      mockEvidenciasService.listarPorIncidente.mockResolvedValue([mockDto]);

      const result = await controller.listarPorIncidente('inc-1');

      expect(mockEvidenciasService.listarPorIncidente).toHaveBeenCalledWith('inc-1');
      expect(result).toEqual([mockDto]);
    });

    it('lista las evidencias asociadas a un incidente por query param', async () => {
      mockEvidenciasService.listarPorIncidente.mockResolvedValue([mockDto]);

      const result = await controller.listarPorQuery('inc-1');

      expect(mockEvidenciasService.listarPorIncidente).toHaveBeenCalledWith('inc-1');
      expect(result).toEqual([mockDto]);
    });
  });

  describe('obtenerMetadata (GET /evidencias/:id)', () => {
    it('retorna la metadata pública de una evidencia', async () => {
      mockEvidenciasService.obtenerMetadata.mockResolvedValue(mockDto);

      const result = await controller.obtenerMetadata('ev-1');

      expect(mockEvidenciasService.obtenerMetadata).toHaveBeenCalledWith('ev-1');
      expect(result).toEqual(mockDto);
    });
  });

  describe('descargarArchivo (GET /evidencias/:id/archivo)', () => {
    it('retorna StreamableFile con los headers Content-Type y Content-Length', async () => {
      const buffer = Buffer.from('contenido-binario-jpg');
      mockEvidenciasService.obtenerArchivo.mockResolvedValue({
        evidencia: { id: 'ev-1', incidenteId: 'inc-1', tipoMime: 'image/jpeg' },
        archivo: buffer,
        tipoMime: 'image/jpeg',
      });

      const setHeader = jest.fn();
      const res = { setHeader } as unknown as Response;

      const streamable = await controller.descargarArchivo('ev-1', res);

      expect(streamable).toBeInstanceOf(StreamableFile);
      expect(setHeader).toHaveBeenCalledWith('Content-Type', 'image/jpeg');
      expect(setHeader).toHaveBeenCalledWith(
        'Content-Length',
        buffer.length.toString(),
      );
    });

    it('lanza 404 si se solicita por incidenteId pero la evidencia pertenece a otro incidente', async () => {
      mockEvidenciasService.obtenerArchivo.mockResolvedValue({
        evidencia: { id: 'ev-1', incidenteId: 'inc-otro', tipoMime: 'image/jpeg' },
        archivo: Buffer.from('x'),
        tipoMime: 'image/jpeg',
      });

      const res = { setHeader: jest.fn() } as unknown as Response;

      await expect(
        controller.descargarArchivo('ev-1', res, 'inc-1'),
      ).rejects.toBeInstanceOf(NotFoundException);
    });
  });
});
