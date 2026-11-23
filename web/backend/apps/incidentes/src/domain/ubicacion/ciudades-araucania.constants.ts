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

export const normalizar = (valor: string): string =>
  valor
    .trim()
    .toLocaleLowerCase('es')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '');

/** Límites aproximados usados por el mapa web para acotar La Araucanía. */
export const LIMITES_ARAUCANIA = {
  latitudMinima: -39.7,
  latitudMaxima: -37.4,
  longitudMinima: -73.9,
  longitudMaxima: -71.0,
} as const;

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

export function esComunaDeAraucania(comuna: string): boolean {
  const objetivo = normalizar(comuna);
  return COMUNAS_ARAUCANIA.some((c) => normalizar(c) === objetivo);
}

export function esRegionAraucania(region: string): boolean {
  const objetivo = normalizar(region);
  return objetivo === 'araucania' || objetivo === 'laaraucania';
}

export function coordenadasDentroDeAraucania(
  latitud: number,
  longitud: number,
): boolean {
  return (
    latitud >= LIMITES_ARAUCANIA.latitudMinima &&
    latitud <= LIMITES_ARAUCANIA.latitudMaxima &&
    longitud >= LIMITES_ARAUCANIA.longitudMinima &&
    longitud <= LIMITES_ARAUCANIA.longitudMaxima
  );
}