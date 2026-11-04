import { PrismaPg } from '@prisma/adapter-pg';
import { Pool } from 'pg';
import { Prisma, PrismaClient } from '../src/generated/prisma/client';
import {
  ESTADOS_INCIDENTE,
  type EstadoIncidente,
} from '../src/estado/transiciones-incidente.constants';
import {
  esPrioridadValida,
  PRIORIDAD_MAXIMA,
  PRIORIDAD_MINIMA,
} from '../src/prioridad/prioridad.constants';

/**
 * Seed idempotente de incidentes demo georreferenciados en La Araucanía
 * (Issue #152).
 *
 * - Reutiliza los catálogos de estados y categorías del microservicio.
 * - Asocia los incidentes a los UUID fijos de los ciudadanos demo de #151.
 * - Usa UUID fijos y comparación campo a campo para no duplicar ni reescribir
 *   ubicaciones e incidentes en reejecuciones.
 * - Valida los ciudadanos demo en la base de auth-usuarios antes de abrir la
 *   transacción de escritura. Si faltan, aborta sin modificar incidentes.
 */

const REGION = 'La Araucanía';

/** UUID fijos de los ciudadanos demo definidos en el seed de auth (#151). */
const CIUDADANOS_DEMO = [
  'a1b2c3d4-0001-4000-8000-000000000001',
  'a1b2c3d4-0002-4000-8000-000000000002',
  'a1b2c3d4-0003-4000-8000-000000000003',
] as const;

/** Límites aproximados usados por el mapa web para acotar La Araucanía. */
const ARAUCANIA_BOUNDS = {
  latitudMinima: -39.7,
  latitudMaxima: -37.4,
  longitudMinima: -73.9,
  longitudMaxima: -71.0,
} as const;

const UUID_REGEX =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

interface EstadoDemo {
  nombre: EstadoIncidente;
  orden: number;
}

interface CategoriaDemo {
  nombre: string;
  descripcion: string;
  prioridadBase: number;
  esEmergenciaCritica: boolean;
  canalOficialContacto: string | null;
}

interface UbicacionDemo {
  id: string;
  latitud: number;
  longitud: number;
  direccionReferencia: string;
  comuna: string;
  region: string;
  sector: string;
}

interface IncidenteDemo {
  id: string;
  titulo: string;
  descripcion: string;
  categoria: string;
  estado: EstadoIncidente;
  ubicacionId: string;
  usuarioReportaId: string;
  fechaCreacion: string;
}

interface EstadoAsegurado {
  id: string;
  nombre: string;
  orden: number;
}

interface CategoriaAsegurada {
  id: string;
  nombre: string;
  prioridadBase: number;
}

type ResultadoEscritura = 'creado' | 'actualizado' | 'sin-cambios';

interface ResumenEscritura {
  creados: number;
  actualizados: number;
  sinCambios: number;
}

class SeedError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'SeedError';
  }
}

const ESTADOS_DEMO: readonly EstadoDemo[] = [
  { nombre: 'Nuevo', orden: 1 },
  { nombre: 'En revisión', orden: 2 },
  { nombre: 'En atención', orden: 3 },
  { nombre: 'Resuelto', orden: 4 },
  { nombre: 'Cerrado', orden: 5 },
];

const CATEGORIAS_DEMO: readonly CategoriaDemo[] = [
  {
    nombre: 'Alumbrado público',
    descripcion: 'Fallas de alumbrado en calles y espacios públicos',
    prioridadBase: 2,
    esEmergenciaCritica: false,
    canalOficialContacto: null,
  },
  {
    nombre: 'Baches y vialidad',
    descripcion: 'Baches, calzada y señalización en mal estado',
    prioridadBase: 2,
    esEmergenciaCritica: false,
    canalOficialContacto: null,
  },
  {
    nombre: 'Aseo y basura',
    descripcion: 'Microbasurales y recolección pendiente',
    prioridadBase: 1,
    esEmergenciaCritica: false,
    canalOficialContacto: null,
  },
  {
    nombre: 'Agua y alcantarillado',
    descripcion: 'Fugas, cortes de agua y alcantarillado colapsado',
    prioridadBase: 3,
    esEmergenciaCritica: false,
    canalOficialContacto: null,
  },
  {
    nombre: 'Áreas verdes y arbolado',
    descripcion: 'Plazas, poda y arbolado en riesgo',
    prioridadBase: 1,
    esEmergenciaCritica: false,
    canalOficialContacto: null,
  },
  {
    nombre: 'Seguridad ciudadana',
    descripcion: 'Hechos delictuales o situaciones de riesgo',
    prioridadBase: 4,
    esEmergenciaCritica: false,
    canalOficialContacto: 'Carabineros 133',
  },
  {
    nombre: 'Accidente de tránsito',
    descripcion: 'Choques y atropellos con personas afectadas',
    prioridadBase: 5,
    esEmergenciaCritica: true,
    canalOficialContacto: 'SAMU 131',
  },
  {
    nombre: 'Incendio',
    descripcion: 'Incendios estructurales, forestales o pastizales',
    prioridadBase: 5,
    esEmergenciaCritica: true,
    canalOficialContacto: 'Bomberos 132',
  },
];

const UBICACIONES_DEMO: readonly UbicacionDemo[] = [
  {
    id: 'd1520000-0000-4000-8000-000000000001',
    latitud: -38.7368,
    longitud: -72.6065,
    direccionReferencia: 'Av. Alemania 01234',
    comuna: 'Temuco',
    region: REGION,
    sector: 'Centro',
  },
  {
    id: 'd1520000-0000-4000-8000-000000000002',
    latitud: -38.7445,
    longitud: -72.618,
    direccionReferencia: 'Calle Las Heras 0456',
    comuna: 'Temuco',
    region: REGION,
    sector: 'Pedro de Valdivia',
  },
  {
    id: 'd1520000-0000-4000-8000-000000000003',
    latitud: -38.763,
    longitud: -72.598,
    direccionReferencia: 'Pasaje Los Aromos 0123',
    comuna: 'Padre Las Casas',
    region: REGION,
    sector: 'Centro',
  },
  {
    id: 'd1520000-0000-4000-8000-000000000004',
    latitud: -38.742,
    longitud: -72.595,
    direccionReferencia: 'Calle Caupolicán 0789',
    comuna: 'Temuco',
    region: REGION,
    sector: 'Santa Rosa',
  },
  {
    id: 'd1520000-0000-4000-8000-000000000005',
    latitud: -38.771,
    longitud: -72.585,
    direccionReferencia: 'Av. Balmaceda 0456',
    comuna: 'Padre Las Casas',
    region: REGION,
    sector: 'San Antonio',
  },
  {
    id: 'd1520000-0000-4000-8000-000000000006',
    latitud: -38.755,
    longitud: -72.625,
    direccionReferencia: 'Av. Los Poetas 0234',
    comuna: 'Temuco',
    region: REGION,
    sector: 'Amanecer',
  },
  {
    id: 'd1520000-0000-4000-8000-000000000007',
    latitud: -39.282,
    longitud: -72.227,
    direccionReferencia: 'Av. Pedro de Valdivia 0567',
    comuna: 'Villarrica',
    region: REGION,
    sector: 'Centro',
  },
  {
    id: 'd1520000-0000-4000-8000-000000000008',
    latitud: -37.798,
    longitud: -72.708,
    direccionReferencia: 'Camino El Vergel km 3',
    comuna: 'Angol',
    region: REGION,
    sector: 'El Vergel',
  },
  {
    id: 'd1520000-0000-4000-8000-000000000009',
    latitud: -38.744,
    longitud: -72.95,
    direccionReferencia: 'Calle Manuel Montt 0345',
    comuna: 'Nueva Imperial',
    region: REGION,
    sector: 'Centro',
  },
  {
    id: 'd1520000-0000-4000-8000-000000000010',
    latitud: -38.78,
    longitud: -72.665,
    direccionReferencia: 'Ruta S-40 km 12',
    comuna: 'Temuco',
    region: REGION,
    sector: 'Labranza',
  },
];

const INCIDENTES_DEMO: readonly IncidenteDemo[] = [
  {
    id: 'e1520000-0000-4000-8000-000000000001',
    titulo: 'Luminaria intermitente en Av. Alemania',
    descripcion:
      'La luminaria frente al número 01234 parpadea durante la noche y deja a oscuras el paso peatonal.',
    categoria: 'Alumbrado público',
    estado: 'En revisión',
    ubicacionId: 'd1520000-0000-4000-8000-000000000001',
    usuarioReportaId: 'a1b2c3d4-0001-4000-8000-000000000001',
    fechaCreacion: '2026-11-02T12:15:00.000Z',
  },
  {
    id: 'e1520000-0000-4000-8000-000000000002',
    titulo: 'Bache profundo en calle Las Heras',
    descripcion:
      'Bache de aproximadamente 40 cm de diámetro en la calzada, con riesgo para ciclistas y vehículos menores.',
    categoria: 'Baches y vialidad',
    estado: 'En atención',
    ubicacionId: 'd1520000-0000-4000-8000-000000000002',
    usuarioReportaId: 'a1b2c3d4-0002-4000-8000-000000000002',
    fechaCreacion: '2026-11-01T14:40:00.000Z',
  },
  {
    id: 'e1520000-0000-4000-8000-000000000003',
    titulo: 'Microbasural en pasaje Los Aromos',
    descripcion:
      'Acumulación de bolsas y escombros en la esquina del pasaje, con presencia de roedores.',
    categoria: 'Aseo y basura',
    estado: 'Nuevo',
    ubicacionId: 'd1520000-0000-4000-8000-000000000003',
    usuarioReportaId: 'a1b2c3d4-0003-4000-8000-000000000003',
    fechaCreacion: '2026-10-31T11:05:00.000Z',
  },
  {
    id: 'e1520000-0000-4000-8000-000000000004',
    titulo: 'Fuga de agua en calle Caupolicán',
    descripcion:
      'Fuga constante en la vereda que está erosionando el pavimento y afectando el suministro de los vecinos.',
    categoria: 'Agua y alcantarillado',
    estado: 'En atención',
    ubicacionId: 'd1520000-0000-4000-8000-000000000004',
    usuarioReportaId: 'a1b2c3d4-0001-4000-8000-000000000001',
    fechaCreacion: '2026-10-30T16:20:00.000Z',
  },
  {
    id: 'e1520000-0000-4000-8000-000000000005',
    titulo: 'Árbol con riesgo de caída en plaza San Antonio',
    descripcion:
      'Árbol de gran altura con ramas secas sobre el sector de juegos infantiles; se solicitó poda o retiro.',
    categoria: 'Áreas verdes y arbolado',
    estado: 'Resuelto',
    ubicacionId: 'd1520000-0000-4000-8000-000000000005',
    usuarioReportaId: 'a1b2c3d4-0002-4000-8000-000000000002',
    fechaCreacion: '2026-10-29T13:00:00.000Z',
  },
  {
    id: 'e1520000-0000-4000-8000-000000000006',
    titulo: 'Robo con intimidación en parada de buses',
    descripcion:
      'Reporte de robo con intimidación en la parada de Av. Los Poetas; se solicita patrullaje preventivo.',
    categoria: 'Seguridad ciudadana',
    estado: 'Cerrado',
    ubicacionId: 'd1520000-0000-4000-8000-000000000006',
    usuarioReportaId: 'a1b2c3d4-0003-4000-8000-000000000003',
    fechaCreacion: '2026-10-28T23:10:00.000Z',
  },
  {
    id: 'e1520000-0000-4000-8000-000000000007',
    titulo: 'Colisión vehicular en Av. Pedro de Valdivia',
    descripcion:
      'Colisión entre dos vehículos en la intersección, con un ocupante lesionado y congestión vehicular.',
    categoria: 'Accidente de tránsito',
    estado: 'En revisión',
    ubicacionId: 'd1520000-0000-4000-8000-000000000007',
    usuarioReportaId: 'a1b2c3d4-0001-4000-8000-000000000001',
    fechaCreacion: '2026-10-27T18:45:00.000Z',
  },
  {
    id: 'e1520000-0000-4000-8000-000000000008',
    titulo: 'Incendio de pastizal en sector El Vergel',
    descripcion:
      'Incendio de pastizal en un terreno rural, controlado por Bomberos; se solicita revisión de focos secundarios.',
    categoria: 'Incendio',
    estado: 'Resuelto',
    ubicacionId: 'd1520000-0000-4000-8000-000000000008',
    usuarioReportaId: 'a1b2c3d4-0002-4000-8000-000000000002',
    fechaCreacion: '2026-10-26T20:30:00.000Z',
  },
  {
    id: 'e1520000-0000-4000-8000-000000000009',
    titulo: 'Recolección de basura atrasada en calle Manuel Montt',
    descripcion:
      'Los contenedores de la cuadra llevan cuatro días sin recolección y presentan malos olores.',
    categoria: 'Aseo y basura',
    estado: 'Nuevo',
    ubicacionId: 'd1520000-0000-4000-8000-000000000009',
    usuarioReportaId: 'a1b2c3d4-0003-4000-8000-000000000003',
    fechaCreacion: '2026-10-25T10:00:00.000Z',
  },
  {
    id: 'e1520000-0000-4000-8000-000000000010',
    titulo: 'Poste de alumbrado dañado en Ruta S-40',
    descripcion:
      'Poste inclinado y sin luminaria en el kilómetro 12; el sector queda sin iluminación durante la noche.',
    categoria: 'Alumbrado público',
    estado: 'Cerrado',
    ubicacionId: 'd1520000-0000-4000-8000-000000000010',
    usuarioReportaId: 'a1b2c3d4-0001-4000-8000-000000000001',
    fechaCreacion: '2026-10-24T15:30:00.000Z',
  },
];

function mensajeDeError(error: unknown): string {
  return error instanceof Error ? error.message : String(error);
}

function nuevoResumen(): ResumenEscritura {
  return { creados: 0, actualizados: 0, sinCambios: 0 };
}

function registrarResultado(
  resumen: ResumenEscritura,
  resultado: ResultadoEscritura,
): void {
  if (resultado === 'creado') {
    resumen.creados += 1;
  } else if (resultado === 'actualizado') {
    resumen.actualizados += 1;
  } else {
    resumen.sinCambios += 1;
  }
}

function formatearResumen(resumen: ResumenEscritura): string {
  return `creados=${resumen.creados}, actualizados=${resumen.actualizados}, sin cambios=${resumen.sinCambios}`;
}

function validarUuid(valor: string, contexto: string): void {
  if (!UUID_REGEX.test(valor)) {
    throw new SeedError(`${contexto}: "${valor}" no es un UUID válido.`);
  }
}

/**
 * Valida las definiciones locales antes de abrir cualquier conexión. Así un
 * error de datos del propio seed nunca deja escrituras parciales.
 */
function validarDefinicionesDemo(): void {
  const estados = new Set<string>();
  for (const estado of ESTADOS_DEMO) {
    if (estados.has(estado.nombre)) {
      throw new SeedError(`Estado demo duplicado: "${estado.nombre}".`);
    }
    if (!ESTADOS_INCIDENTE.includes(estado.nombre)) {
      throw new SeedError(
        `Estado demo no reconocido por el dominio: "${estado.nombre}".`,
      );
    }
    if (!Number.isInteger(estado.orden) || estado.orden <= 0) {
      throw new SeedError(
        `El estado "${estado.nombre}" tiene un orden inválido: ${estado.orden}.`,
      );
    }
    estados.add(estado.nombre);
  }

  for (const nombre of ESTADOS_INCIDENTE) {
    if (!estados.has(nombre)) {
      throw new SeedError(`Falta el estado canónico "${nombre}" en el seed.`);
    }
  }

  const categorias = new Set<string>();
  for (const categoria of CATEGORIAS_DEMO) {
    if (categorias.has(categoria.nombre)) {
      throw new SeedError(`Categoría demo duplicada: "${categoria.nombre}".`);
    }
    if (!categoria.descripcion.trim()) {
      throw new SeedError(
        `La categoría "${categoria.nombre}" no tiene descripción.`,
      );
    }
    if (!esPrioridadValida(categoria.prioridadBase)) {
      throw new SeedError(
        `La categoría "${categoria.nombre}" tiene prioridadBase ${categoria.prioridadBase}; debe ser un entero entre ${PRIORIDAD_MINIMA} y ${PRIORIDAD_MAXIMA}.`,
      );
    }
    categorias.add(categoria.nombre);
  }

  const ubicaciones = new Set<string>();
  for (const ubicacion of UBICACIONES_DEMO) {
    validarUuid(ubicacion.id, 'Ubicación demo');
    if (ubicaciones.has(ubicacion.id)) {
      throw new SeedError(`Ubicación demo duplicada: "${ubicacion.id}".`);
    }
    if (
      !Number.isFinite(ubicacion.latitud) ||
      !Number.isFinite(ubicacion.longitud)
    ) {
      throw new SeedError(
        `La ubicación "${ubicacion.id}" tiene coordenadas no numéricas.`,
      );
    }
    if (
      ubicacion.latitud < ARAUCANIA_BOUNDS.latitudMinima ||
      ubicacion.latitud > ARAUCANIA_BOUNDS.latitudMaxima ||
      ubicacion.longitud < ARAUCANIA_BOUNDS.longitudMinima ||
      ubicacion.longitud > ARAUCANIA_BOUNDS.longitudMaxima
    ) {
      throw new SeedError(
        `La ubicación "${ubicacion.id}" (${ubicacion.latitud}, ${ubicacion.longitud}) está fuera de los límites de La Araucanía.`,
      );
    }
    if (ubicacion.region !== REGION) {
      throw new SeedError(
        `La ubicación "${ubicacion.id}" debe pertenecer a ${REGION}.`,
      );
    }
    if (
      !ubicacion.direccionReferencia.trim() ||
      !ubicacion.comuna.trim() ||
      !ubicacion.sector.trim()
    ) {
      throw new SeedError(
        `La ubicación "${ubicacion.id}" tiene campos de dirección vacíos.`,
      );
    }
    ubicaciones.add(ubicacion.id);
  }

  const ciudadanos = new Set<string>(CIUDADANOS_DEMO);
  const incidentes = new Set<string>();
  for (const incidente of INCIDENTES_DEMO) {
    validarUuid(incidente.id, 'Incidente demo');
    if (incidentes.has(incidente.id)) {
      throw new SeedError(`Incidente demo duplicado: "${incidente.id}".`);
    }
    if (!incidente.titulo.trim() || !incidente.descripcion.trim()) {
      throw new SeedError(
        `El incidente "${incidente.id}" no tiene título o descripción.`,
      );
    }
    if (!categorias.has(incidente.categoria)) {
      throw new SeedError(
        `El incidente "${incidente.id}" referencia la categoría inexistente "${incidente.categoria}".`,
      );
    }
    if (!estados.has(incidente.estado)) {
      throw new SeedError(
        `El incidente "${incidente.id}" referencia el estado no reconocido "${incidente.estado}".`,
      );
    }
    if (!ubicaciones.has(incidente.ubicacionId)) {
      throw new SeedError(
        `El incidente "${incidente.id}" referencia la ubicación inexistente "${incidente.ubicacionId}".`,
      );
    }
    if (!ciudadanos.has(incidente.usuarioReportaId)) {
      throw new SeedError(
        `El incidente "${incidente.id}" referencia al ciudadano no demo "${incidente.usuarioReportaId}".`,
      );
    }
    if (Number.isNaN(new Date(incidente.fechaCreacion).getTime())) {
      throw new SeedError(
        `El incidente "${incidente.id}" tiene una fechaCreacion inválida: "${incidente.fechaCreacion}".`,
      );
    }
    incidentes.add(incidente.id);
  }
}

/**
 * Verifica en la base de auth-usuarios que los tres ciudadanos demo existan y
 * tengan el rol CIUDADANO. Se ejecuta antes de la transacción de incidentes.
 */
async function validarCiudadanosDemo(): Promise<void> {
  const connectionString = process.env.DATABASE_URL_AUTH;
  if (!connectionString) {
    throw new SeedError(
      'DATABASE_URL_AUTH no está definida. Este seed valida los ciudadanos demo de la Issue #151 en la base de auth-usuarios; define la variable en web/backend/.env y ejecuta primero el seed de auth-usuarios.',
    );
  }

  const authPool = new Pool({ connectionString });

  try {
    const { rows } = await authPool.query<{
      id: string;
      roles: string[] | null;
    }>(
      `SELECT u.id, array_agg(r.nombre) AS roles
         FROM usuario u
         LEFT JOIN usuario_rol ur ON ur.usuario_id = u.id
         LEFT JOIN rol r ON r.id = ur.rol_id
        WHERE u.id = ANY($1::uuid[])
        GROUP BY u.id`,
      [[...CIUDADANOS_DEMO]],
    );

    const rolesPorId = new Map(
      rows.map((fila) => [fila.id, fila.roles ?? []]),
    );

    const faltantes = CIUDADANOS_DEMO.filter((id) => !rolesPorId.has(id));
    if (faltantes.length > 0) {
      throw new SeedError(
        `Faltan ciudadanos demo en auth-usuarios: ${faltantes.join(', ')}. Ejecuta "cd web/backend/apps/auth-usuarios && npx prisma db seed" (Issue #151) antes de este seed.`,
      );
    }

    const sinRolCiudadano = CIUDADANOS_DEMO.filter(
      (id) => !rolesPorId.get(id)?.includes('CIUDADANO'),
    );
    if (sinRolCiudadano.length > 0) {
      throw new SeedError(
        `Los siguientes ciudadanos demo no tienen el rol CIUDADANO: ${sinRolCiudadano.join(', ')}. Vuelve a ejecutar el seed de auth-usuarios (#151).`,
      );
    }
  } catch (error) {
    if (error instanceof SeedError) {
      throw error;
    }
    throw new SeedError(
      `No se pudieron validar los ciudadanos demo en auth-usuarios: ${mensajeDeError(error)}. Verifica DATABASE_URL_AUTH y que el seed de #151 se haya ejecutado.`,
    );
  } finally {
    await authPool.end().catch(() => undefined);
  }
}

async function asegurarEstados(
  tx: Prisma.TransactionClient,
): Promise<{ porNombre: Map<string, EstadoAsegurado>; resumen: ResumenEscritura }> {
  const porNombre = new Map<string, EstadoAsegurado>();
  const resumen = nuevoResumen();

  for (const estado of ESTADOS_DEMO) {
    const existente = await tx.estado.findUnique({
      where: { nombre: estado.nombre },
    });

    if (!existente) {
      const creado = await tx.estado.create({ data: estado });
      porNombre.set(creado.nombre, {
        id: creado.id,
        nombre: creado.nombre,
        orden: creado.orden,
      });
      registrarResultado(resumen, 'creado');
      continue;
    }

    if (existente.orden !== estado.orden) {
      const actualizado = await tx.estado.update({
        where: { id: existente.id },
        data: { orden: estado.orden },
      });
      porNombre.set(actualizado.nombre, {
        id: actualizado.id,
        nombre: actualizado.nombre,
        orden: actualizado.orden,
      });
      registrarResultado(resumen, 'actualizado');
      continue;
    }

    porNombre.set(existente.nombre, {
      id: existente.id,
      nombre: existente.nombre,
      orden: existente.orden,
    });
    registrarResultado(resumen, 'sin-cambios');
  }

  return { porNombre, resumen };
}

async function asegurarCategorias(
  tx: Prisma.TransactionClient,
): Promise<{
  porNombre: Map<string, CategoriaAsegurada>;
  resumen: ResumenEscritura;
}> {
  const porNombre = new Map<string, CategoriaAsegurada>();
  const resumen = nuevoResumen();

  for (const categoria of CATEGORIAS_DEMO) {
    const existentes = await tx.categoria.findMany({
      where: { nombre: categoria.nombre },
      orderBy: { id: 'asc' },
    });

    if (existentes.length === 0) {
      const creada = await tx.categoria.create({ data: categoria });
      porNombre.set(creada.nombre, {
        id: creada.id,
        nombre: creada.nombre,
        prioridadBase: creada.prioridadBase,
      });
      registrarResultado(resumen, 'creado');
      continue;
    }

    if (existentes.length > 1) {
      console.warn(
        `Aviso: existen ${existentes.length} categorías con el nombre "${categoria.nombre}"; se usa la de id ${existentes[0].id}.`,
      );
    }

    const [principal] = existentes;
    const requiereActualizacion =
      principal.descripcion !== categoria.descripcion ||
      principal.prioridadBase !== categoria.prioridadBase ||
      principal.esEmergenciaCritica !== categoria.esEmergenciaCritica ||
      principal.canalOficialContacto !== categoria.canalOficialContacto;

    if (requiereActualizacion) {
      const actualizada = await tx.categoria.update({
        where: { id: principal.id },
        data: categoria,
      });
      porNombre.set(actualizada.nombre, {
        id: actualizada.id,
        nombre: actualizada.nombre,
        prioridadBase: actualizada.prioridadBase,
      });
      registrarResultado(resumen, 'actualizado');
      continue;
    }

    porNombre.set(principal.nombre, {
      id: principal.id,
      nombre: principal.nombre,
      prioridadBase: principal.prioridadBase,
    });
    registrarResultado(resumen, 'sin-cambios');
  }

  return { porNombre, resumen };
}

/**
 * Comprueba que los catálogos recién asegurados sean utilizables por el
 * dominio antes de crear ubicaciones e incidentes.
 */
function validarCatalogosAsegurados(
  estados: Map<string, EstadoAsegurado>,
  categorias: Map<string, CategoriaAsegurada>,
): void {
  for (const nombre of ESTADOS_INCIDENTE) {
    if (!estados.has(nombre)) {
      throw new SeedError(
        `No se pudo asegurar el estado requerido "${nombre}" en la base de incidentes.`,
      );
    }
  }

  for (const definicion of CATEGORIAS_DEMO) {
    const categoria = categorias.get(definicion.nombre);
    if (!categoria) {
      throw new SeedError(
        `No se pudo asegurar la categoría requerida "${definicion.nombre}" en la base de incidentes.`,
      );
    }
    if (!esPrioridadValida(categoria.prioridadBase)) {
      throw new SeedError(
        `La categoría "${definicion.nombre}" tiene prioridadBase ${categoria.prioridadBase}; debe estar entre ${PRIORIDAD_MINIMA} y ${PRIORIDAD_MAXIMA}.`,
      );
    }
    if (categoria.prioridadBase !== definicion.prioridadBase) {
      throw new SeedError(
        `La categoría "${definicion.nombre}" tiene prioridadBase ${categoria.prioridadBase}, pero el seed espera ${definicion.prioridadBase}.`,
      );
    }
  }
}

async function asegurarUbicaciones(
  tx: Prisma.TransactionClient,
): Promise<{ porId: Map<string, string>; resumen: ResumenEscritura }> {
  const porId = new Map<string, string>();
  const resumen = nuevoResumen();

  for (const ubicacion of UBICACIONES_DEMO) {
    const existente = await tx.ubicacion.findUnique({
      where: { id: ubicacion.id },
    });

    if (!existente) {
      await tx.ubicacion.create({ data: ubicacion });
      porId.set(ubicacion.id, ubicacion.id);
      registrarResultado(resumen, 'creado');
      continue;
    }

    const requiereActualizacion =
      existente.latitud !== ubicacion.latitud ||
      existente.longitud !== ubicacion.longitud ||
      existente.direccionReferencia !== ubicacion.direccionReferencia ||
      existente.comuna !== ubicacion.comuna ||
      existente.region !== ubicacion.region ||
      existente.sector !== ubicacion.sector;

    if (requiereActualizacion) {
      const { id, ...datos } = ubicacion;
      await tx.ubicacion.update({
        where: { id },
        data: datos,
      });
      registrarResultado(resumen, 'actualizado');
    } else {
      registrarResultado(resumen, 'sin-cambios');
    }

    porId.set(ubicacion.id, ubicacion.id);
  }

  return { porId, resumen };
}

async function asegurarIncidentes(
  tx: Prisma.TransactionClient,
  estados: Map<string, EstadoAsegurado>,
  categorias: Map<string, CategoriaAsegurada>,
): Promise<ResumenEscritura> {
  const resumen = nuevoResumen();

  for (const incidente of INCIDENTES_DEMO) {
    const categoria = categorias.get(incidente.categoria);
    if (!categoria) {
      throw new SeedError(
        `No se encontró la categoría "${incidente.categoria}" para el incidente "${incidente.id}".`,
      );
    }

    const estado = estados.get(incidente.estado);
    if (!estado) {
      throw new SeedError(
        `No se encontró el estado "${incidente.estado}" para el incidente "${incidente.id}".`,
      );
    }

    // Misma regla que IncidentesService.create: la prioridad inicial siempre
    // se deriva de la categoría y debe ser un valor válido de la escala.
    const nivelPrioridad = categoria.prioridadBase;
    if (!esPrioridadValida(nivelPrioridad)) {
      throw new SeedError(
        `La categoría "${categoria.nombre}" no tiene una prioridad base válida (${nivelPrioridad}).`,
      );
    }

    const datos = {
      titulo: incidente.titulo,
      descripcion: incidente.descripcion,
      categoriaId: categoria.id,
      estadoId: estado.id,
      ubicacionId: incidente.ubicacionId,
      usuarioReportaId: incidente.usuarioReportaId,
      nivelPrioridad,
      fechaCreacion: new Date(incidente.fechaCreacion),
    };

    const existente = await tx.incidente.findUnique({
      where: { id: incidente.id },
    });

    if (!existente) {
      await tx.incidente.create({
        data: { id: incidente.id, ...datos },
      });
      registrarResultado(resumen, 'creado');
      continue;
    }

    const requiereActualizacion =
      existente.titulo !== datos.titulo ||
      existente.descripcion !== datos.descripcion ||
      existente.categoriaId !== datos.categoriaId ||
      existente.estadoId !== datos.estadoId ||
      existente.ubicacionId !== datos.ubicacionId ||
      existente.usuarioReportaId !== datos.usuarioReportaId ||
      existente.nivelPrioridad !== datos.nivelPrioridad ||
      existente.fechaCreacion.getTime() !== datos.fechaCreacion.getTime();

    if (requiereActualizacion) {
      await tx.incidente.update({
        where: { id: incidente.id },
        data: datos,
      });
      registrarResultado(resumen, 'actualizado');
    } else {
      registrarResultado(resumen, 'sin-cambios');
    }
  }

  return resumen;
}

async function ejecutarSeed(): Promise<void> {
  validarDefinicionesDemo();

  const connectionString = process.env.DATABASE_URL_INCIDENTES;
  if (!connectionString) {
    throw new SeedError(
      'DATABASE_URL_INCIDENTES no está definida. Configúrala en web/backend/.env antes de ejecutar el seed.',
    );
  }

  // La validación de usuarios es de solo lectura y ocurre antes de la
  // transacción: si faltan ciudadanos demo, la base de incidentes no cambia.
  await validarCiudadanosDemo();

  const pool = new Pool({ connectionString });
  const prisma = new PrismaClient({ adapter: new PrismaPg(pool) });

  try {
    const resultado = await prisma.$transaction(
      async (tx) => {
        const estados = await asegurarEstados(tx);
        const categorias = await asegurarCategorias(tx);

        validarCatalogosAsegurados(
          estados.porNombre,
          categorias.porNombre,
        );

        const ubicaciones = await asegurarUbicaciones(tx);
        const incidentes = await asegurarIncidentes(
          tx,
          estados.porNombre,
          categorias.porNombre,
        );

        return { estados, categorias, ubicaciones, incidentes };
      },
      { maxWait: 10000, timeout: 60000 },
    );

    console.log('');
    console.log('Seed de incidentes demo (#152) completado');
    console.log(`  Estados:     ${formatearResumen(resultado.estados.resumen)}`);
    console.log(
      `  Categorías:  ${formatearResumen(resultado.categorias.resumen)}`,
    );
    console.log(
      `  Ubicaciones: ${formatearResumen(resultado.ubicaciones.resumen)}`,
    );
    console.log(`  Incidentes:  ${formatearResumen(resultado.incidentes)}`);
    console.log(
      `  Ciudadanos demo validados en auth-usuarios: ${CIUDADANOS_DEMO.length}`,
    );
    console.log('');
  } finally {
    await prisma.$disconnect().catch(() => undefined);
    await pool.end().catch(() => undefined);
  }
}

ejecutarSeed().catch((error: unknown) => {
  if (error instanceof SeedError) {
    console.error(`\n[seed incidentes #152] ${error.message}\n`);
  } else {
    console.error('\n[seed incidentes #152] Error inesperado:', error);
  }
  process.exitCode = 1;
});
