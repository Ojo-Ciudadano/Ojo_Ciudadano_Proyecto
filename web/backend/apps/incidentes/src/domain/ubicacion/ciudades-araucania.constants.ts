export const COMUNAS_ARAUCANIA = [
  'Angol', 'Carahue', 'Cholchol', 'Collipulli', 'Cunco', 'Curacautín',
  'Curarrehue', 'Ercilla', 'Freire', 'Galvarino', 'Gorbea', 'Lautaro',
  'Loncoche', 'Lonquimay', 'Los Sauces', 'Lumaco', 'Melipeuco',
  'Nueva Imperial', 'Padre Las Casas', 'Perquenco', 'Pitrufquén', 'Pucón',
  'Purén', 'Renaico', 'Saavedra', 'Temuco', 'Teodoro Schmidt', 'Toltén',
  'Traiguén', 'Victoria', 'Vilcún', 'Villarrica',
] as const;

const base = Object.fromEntries(
  COMUNAS_ARAUCANIA.map((comuna) => [comuna, [comuna] as readonly string[]]),
) as Record<string, readonly string[]>;

export const CIUDADES_ARAUCANIA: Readonly<Record<string, readonly string[]>> = {
  ...base,
  Temuco: ['Temuco', 'Padre Las Casas'],
  Villarrica: ['Villarrica', 'Pucón'],
};

const normalizar = (valor: string): string =>
  valor
    .trim()
    .toLocaleLowerCase('es')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '');

export function comunasDeCiudad(ciudad: string): readonly string[] {
  const clave = normalizar(ciudad);
  return (
    Object.entries(CIUDADES_ARAUCANIA).find(
      ([nombre]) => normalizar(nombre) === clave,
    )?.[1] ?? []
  );
}

export function comunaPerteneceACiudad(comuna: string, ciudad: string): boolean {
  const objetivo = normalizar(comuna);
  return comunasDeCiudad(ciudad).some((c) => normalizar(c) === objetivo);
}