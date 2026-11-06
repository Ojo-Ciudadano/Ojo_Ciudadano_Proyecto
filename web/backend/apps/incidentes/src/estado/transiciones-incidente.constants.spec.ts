import {
  ESTADOS_INCIDENTE,
  esEstadoIncidente,
  esTransicionPermitida,
} from './transiciones-incidente.constants';

describe('Transiciones de estado de incidentes (#116)', () => {
  it('mantiene el catalogo canonico de estados sin agregar ni renombrar', () => {
    expect(ESTADOS_INCIDENTE).toEqual([
      'Nuevo',
      'En revisión',
      'En atención',
      'Resuelto',
      'Cerrado',
    ]);
  });

  it.each([
    ['Nuevo', 'En revisión'],
    ['En revisión', 'En atención'],
    ['En atención', 'Resuelto'],
    ['Resuelto', 'En atención'],
    ['Resuelto', 'Cerrado'],
  ])('permite la transicion %s -> %s', (origen, destino) => {
    expect(esTransicionPermitida(origen, destino)).toBe(true);
  });

  it.each([
    ['Nuevo', 'Nuevo'],
    ['Nuevo', 'En atención'],
    ['Nuevo', 'Resuelto'],
    ['Nuevo', 'Cerrado'],
    ['En revisión', 'Nuevo'],
    ['En revisión', 'En revisión'],
    ['En revisión', 'Resuelto'],
    ['En revisión', 'Cerrado'],
    ['En atención', 'Nuevo'],
    ['En atención', 'En revisión'],
    ['En atención', 'En atención'],
    ['En atención', 'Cerrado'],
    ['Resuelto', 'Nuevo'],
    ['Resuelto', 'En revisión'],
    ['Resuelto', 'Resuelto'],
    ['Cerrado', 'Nuevo'],
    ['Cerrado', 'En revisión'],
    ['Cerrado', 'En atención'],
    ['Cerrado', 'Resuelto'],
    ['Cerrado', 'Cerrado'],
  ])('prohibe la transicion %s -> %s', (origen, destino) => {
    expect(esTransicionPermitida(origen, destino)).toBe(false);
  });

  it('trata el mismo estado como transicion prohibida', () => {
    for (const estado of ESTADOS_INCIDENTE) {
      expect(esTransicionPermitida(estado, estado)).toBe(false);
    }
  });

  it('rechaza estados desconocidos o con escritura no canonica', () => {
    expect(esEstadoIncidente('En revision')).toBe(false);
    expect(esEstadoIncidente('Pendiente')).toBe(false);
    expect(esEstadoIncidente(undefined)).toBe(false);
    expect(esTransicionPermitida('Nuevo', 'En revision')).toBe(false);
  });
});
