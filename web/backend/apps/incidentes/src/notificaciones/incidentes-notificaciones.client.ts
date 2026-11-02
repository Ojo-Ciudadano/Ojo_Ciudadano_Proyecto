import { Injectable, Logger, Optional } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { isUUID } from 'class-validator';

import type { EstadoIncidente } from '../estado/transiciones-incidente.constants';

export const NOTIFICACIONES_HTTP_TIMEOUT_MS = 5000;

export interface CambioEstadoEvento {
  incidenteId: string;
  titulo?: string;
  usuarioReportaId: string;
  estadoAnterior?: string;
  estadoNuevo: EstadoIncidente | string;
  observacion?: string;
}

export interface AsignacionIncidenteEvento {
  incidenteId: string;
  titulo?: string;
  usuarioAsignadoId: string;
  esReasignacion?: boolean;
}

export interface CrearNotificacionPayload {
  usuarioId: string;
  incidenteId: string;
  mensaje: string;
}

/**
 * Construye una descripción comprensible del cambio de estado de un incidente
 * para ser consultada posteriormente por el ciudadano en la aplicación móvil.
 */
export function construirMensajeCambioEstado(
  evento: CambioEstadoEvento,
): string {
  const tituloLimpio = evento.titulo?.trim();
  const referenciaIncidente = tituloLimpio
    ? `Tu incidente "${tituloLimpio}"`
    : 'Tu incidente reportado';

  const transicion = evento.estadoAnterior
    ? `cambió de estado de "${evento.estadoAnterior}" a "${evento.estadoNuevo}"`
    : `cambió de estado a "${evento.estadoNuevo}"`;

  const observacionLimpia = evento.observacion?.trim();
  if (observacionLimpia) {
    return `${referenciaIncidente} ${transicion}. Observación: ${observacionLimpia}`;
  }

  return `${referenciaIncidente} ${transicion}.`;
}

/**
 * Construye una descripción comprensible cuando se asigna o reasigna un
 * responsable a un incidente.
 */
export function construirMensajeAsignacion(
  evento: AsignacionIncidenteEvento,
): string {
  const tituloLimpio = evento.titulo?.trim();
  const referenciaIncidente = tituloLimpio
    ? `el incidente "${tituloLimpio}"`
    : 'un incidente';

  const accion = evento.esReasignacion
    ? 'Se te ha reasignado como responsable de'
    : 'Se te ha asignado como responsable de';

  return `${accion} ${referenciaIncidente}.`;
}

@Injectable()
export class IncidentesNotificacionesClient {
  private readonly logger = new Logger(IncidentesNotificacionesClient.name);

  constructor(@Optional() private readonly configService?: ConfigService) {}

  getBaseUrl(): string {
    return (
      this.configService?.get<string>('NOTIFICACIONES_SERVICE_URL') ??
      process.env.NOTIFICACIONES_SERVICE_URL ??
      'http://localhost:3003'
    );
  }

  async notificarCambioEstado(evento: CambioEstadoEvento): Promise<boolean> {
    if (
      !isUUID(evento?.usuarioReportaId, 'all') ||
      !isUUID(evento?.incidenteId, 'all') ||
      !evento?.estadoNuevo?.trim()
    ) {
      this.logger.warn(
        `Evento de cambio de estado omitido por datos incompletos para incidente ${evento?.incidenteId ?? 'desconocido'}`,
      );
      return false;
    }

    const mensaje = construirMensajeCambioEstado(evento);
    return this.enviarNotificacion({
      usuarioId: evento.usuarioReportaId,
      incidenteId: evento.incidenteId,
      mensaje,
    });
  }

  async notificarAsignacion(
    evento: AsignacionIncidenteEvento,
  ): Promise<boolean> {
    if (
      !isUUID(evento?.usuarioAsignadoId, 'all') ||
      !isUUID(evento?.incidenteId, 'all')
    ) {
      this.logger.warn(
        `Evento de asignacion omitido por datos incompletos para incidente ${evento?.incidenteId ?? 'desconocido'}`,
      );
      return false;
    }

    const mensaje = construirMensajeAsignacion(evento);
    return this.enviarNotificacion({
      usuarioId: evento.usuarioAsignadoId,
      incidenteId: evento.incidenteId,
      mensaje,
    });
  }

  async enviarNotificacion(
    payload: CrearNotificacionPayload,
  ): Promise<boolean> {
    const baseUrl = this.getBaseUrl().replace(/\/+$/, '');
    const url = `${baseUrl}/notificaciones`;

    try {
      const response = await fetch(url, {
        method: 'POST',
        headers: {
          'content-type': 'application/json',
        },
        body: JSON.stringify(payload),
        signal: AbortSignal.timeout(NOTIFICACIONES_HTTP_TIMEOUT_MS),
      });

      if (!response.ok) {
        this.logger.warn(
          `El microservicio de notificaciones respondio con estado ${response.status} al registrar notificacion para incidente ${payload.incidenteId}`,
        );
        return false;
      }

      return true;
    } catch (error) {
      const detalle =
        error instanceof Error ? error.message : 'Error desconocido';
      this.logger.error(
        `Fallo controlado al enviar notificacion a ${url} para incidente ${payload.incidenteId}: ${detalle}`,
      );
      return false;
    }
  }
}
