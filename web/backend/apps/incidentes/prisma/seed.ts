import { PrismaPg } from '@prisma/adapter-pg';
import { PrismaClient } from '../src/generated/prisma';

const connectionString = process.env.DATABASE_URL_INCIDENTES;
if (!connectionString) {
  throw new Error('DATABASE_URL_INCIDENTES no está definida en web/backend/.env');
}

const adapter = new PrismaPg({ connectionString });
const prisma = new PrismaClient({ adapter });

const categoriasBase = [
  {
    id: '10000000-0000-4000-8000-000000000001',
    nombre: 'Alumbrado Público',
    descripcion: 'Fallas o mejoras en el alumbrado de calles y espacios públicos.',
    prioridadBase: 2,
  },
  {
    id: '10000000-0000-4000-8000-000000000002',
    nombre: 'Basura y Limpieza',
    descripcion: 'Acumulación de basura, escombros o limpieza de espacios públicos.',
    prioridadBase: 2,
  },
  {
    id: '10000000-0000-4000-8000-000000000003',
    nombre: 'Vialidad y Calles',
    descripcion: 'Problemas de calles, veredas, señalización y tránsito.',
    prioridadBase: 2,
  },
  {
    id: '10000000-0000-4000-8000-000000000004',
    nombre: 'Seguridad Ciudadana',
    descripcion: 'Situaciones que requieren atención de seguridad en el espacio público.',
    prioridadBase: 1,
    esEmergenciaCritica: true,
    canalOficialContacto: '133',
  },
  {
    id: '10000000-0000-4000-8000-000000000005',
    nombre: 'Parques y Áreas Verdes',
    descripcion: 'Mantenimiento y problemas en parques, plazas y áreas verdes.',
    prioridadBase: 3,
  },
];

const estadosBase = [
  { nombre: 'PENDIENTE', orden: 1 },
  { nombre: 'EN_PROCESO', orden: 2 },
  { nombre: 'RESUELTO', orden: 3 },
  { nombre: 'RECHAZADO', orden: 4 },
];

async function main() {
  for (const categoria of categoriasBase) {
    const { id, ...data } = categoria;
    await prisma.categoria.upsert({
      where: { id },
      update: data,
      create: categoria,
    });
  }

  for (const estado of estadosBase) {
    await prisma.estado.upsert({
      where: { nombre: estado.nombre },
      update: { orden: estado.orden },
      create: estado,
    });
  }

  console.log(`Categorías aseguradas: ${categoriasBase.map((c) => c.nombre).join(', ')}`);
  console.log(`Estados asegurados: ${estadosBase.map((e) => e.nombre).join(', ')}`);

  const [categorias, estados] = await Promise.all([
    prisma.categoria.count(),
    prisma.estado.count(),
  ]);
  console.log(`Totales en BD -> categorias: ${categorias}, estados: ${estados}`);
}

main()
  .catch((error) => {
    console.error(error);
    process.exit(1);
  })
  .finally(() => prisma.$disconnect());