/// <reference types="jest" />
import { plainToInstance } from 'class-transformer';
import { validate } from 'class-validator';
import { FindIncidentesQueryDto } from './find-incidentes-query.dto';

const validar = async (valor: Record<string, unknown>) => {
  const dto = plainToInstance(FindIncidentesQueryDto, valor);
  return validate(dto);
};

describe('FindIncidentesQueryDto', () => {
  it('acepta una consulta sin filtros', async () => {
    await expect(validar({})).resolves.toHaveLength(0);
  });
  it('acepta todos los filtros con formato valido', async () => {
    const errors = await validar({
      usuarioReportaId: 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
      estadoId: 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14',
      categoriaId: 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
      comuna: '  Temuco  ',
    });
    expect(errors).toHaveLength(0);
  });

  it('recorta los espacios de comuna', async () => {
    const dto = plainToInstance(FindIncidentesQueryDto, {
      comuna: '  Temuco  ',
    });
    expect(dto.comuna).toBe('Temuco');
  });

  it.each(['usuarioReportaId', 'estadoId', 'categoriaId'])(
    'rechaza %s que no es UUID',
    async (campo) => {
      const errors = await validar({ [campo]: 'no-es-uuid' });
      expect(errors).toHaveLength(1);
      expect(errors[0].property).toBe(campo);
    },
  );

  it.each(['', '   '])('rechaza comuna vacia (%p)', async (comuna) => {
    const errors = await validar({ comuna });
    expect(errors).toHaveLength(1);
    expect(errors[0].property).toBe('comuna');
  });

  it('rechaza comuna que no es texto', async () => {
    const errors = await validar({ comuna: 123 });
    expect(errors).toHaveLength(1);
    expect(errors[0].property).toBe('comuna');
  });
});