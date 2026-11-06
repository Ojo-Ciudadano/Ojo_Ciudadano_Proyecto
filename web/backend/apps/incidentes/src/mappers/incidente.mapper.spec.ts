import { toIncidenteResponse } from './incidente.mapper';
import { buildEvidenciaUrl, toEvidenciaResponse } from './evidencia.mapper';

const base = {
  id: 'inc-1',
  titulo: 'Poste caido',
  descripcion: 'desc',
  categoria: { id: 'cat-1' },
  estado: { id: 'est-1' },
  ubicacion: { id: 'ubi-1' },
};

const evidencia = {
  id: 'evi-1',
  incidenteId: 'inc-1',
  tipoMime: 'image/jpeg',
  tamanoArchivo: 2048,
  fechaCreacion: new Date('2026-10-22T12:00:00.000Z'),
};

describe('mappers de evidencias', () => {
  it('construye la referencia relativa de una evidencia', () => {
    expect(buildEvidenciaUrl('evi-1')).toBe('/evidencias/evi-1/archivo');
  });

  it('mapea solo metadata y referencia, sin claveArchivo', () => {
    const dto = toEvidenciaResponse({ ...evidencia, claveArchivo: 'incidentes/inc-1/secreto.jpg' } as any);
    expect(dto).toEqual({ ...evidencia, url: '/evidencias/evi-1/archivo' });
    expect(JSON.stringify(dto)).not.toContain('secreto');
    expect(dto).not.toHaveProperty('claveArchivo');
  });

  it('conserva la asociacion con el incidente', () => {
    const out = toIncidenteResponse({ ...base, evidencias: [evidencia] });
    expect(out.evidencias[0].incidenteId).toBe(out.id);
  });

  it('devuelve evidencias vacio cuando no hay evidencias', () => {
    expect(toIncidenteResponse({ ...base, evidencias: [] }).evidencias).toEqual([]);
  });

  it('devuelve evidencias vacio cuando la relacion no viene cargada', () => {
    expect(toIncidenteResponse({ ...base }).evidencias).toEqual([]);
  });

  it('mantiene los campos existentes del contrato', () => {
    const out = toIncidenteResponse({ ...base, evidencias: [] });
    expect(out).toMatchObject(base);
  });
});