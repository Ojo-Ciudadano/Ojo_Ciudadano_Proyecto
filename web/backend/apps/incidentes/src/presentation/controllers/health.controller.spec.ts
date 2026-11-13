import { HealthController } from './health.controller';

describe('HealthController', () => {
  it('reports process health without checking external dependencies', () => {
    expect(new HealthController().health()).toEqual({ status: 'ok' });
  });
});
