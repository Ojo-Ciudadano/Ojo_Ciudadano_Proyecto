import { toHistorialEstadoResponse } from './historial-estado.mapper';

const FECHA = new Date('2026-10-30T10:00:00.000Z');

const registro = {
  id: 'e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',
  incidenteId: 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
  estadoId: 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a15',
  usuarioId: 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
  observacion: 'Se inicia la evaluacion',
  fecha: FECHA,
  estado: {
    id: 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a15',
    nombre: 'En revisión',
    orden: 2,
  },
};

describe('toHistorialEstadoResponse', () => {
  it('mapea el contrato publico de un registro de historial', () => {
    expect(toHistorialEstadoResponse(registro)).toEqual({
      id: registro.id,
      incidenteId: registro.incidenteId,
      estado: {
        id: registro.estado.id,
        nombre: registro.estado.nombre,
        orden: registro.estado.orden,
      },
      usuarioId: registro.usuarioId,
      observacion: registro.observacion,
      fecha: FECHA,
    });
  });

  it('conserva el actor persistido sin reemplazarlo por otro usuario', () => {
    const dto = toHistorialEstadoResponse(registro);

    expect(dto.usuarioId).toBe(registro.usuarioId);
  });

  it('representa una observacion ausente como null', () => {
    const dto = toHistorialEstadoResponse({ ...registro, observacion: null });

    expect(dto).toHaveProperty('observacion', null);
  });
});