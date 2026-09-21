import { PrismaClient } from '../src/generated/prisma/client';
import { PrismaPg } from '@prisma/adapter-pg';
import { Pool } from 'pg';

const pool = new Pool({ connectionString: process.env.DATABASE_URL_INCIDENTES });
const prisma = new PrismaClient({ adapter: new PrismaPg(pool) });

const estados = ['Nuevo', 'En revisión', 'En atención', 'Resuelto', 'Cerrado'];
const categorias = [
  { nombre: 'Alumbrado público', descripcion: 'Fallas de alumbrado en calles y espacios públicos', prioridadBase: 2, esEmergenciaCritica: false, canalOficialContacto: null },
  { nombre: 'Baches y vialidad', descripcion: 'Baches, calzada y señalización en mal estado', prioridadBase: 2, esEmergenciaCritica: false, canalOficialContacto: null },
  { nombre: 'Aseo y basura', descripcion: 'Microbasurales y recolección pendiente', prioridadBase: 1, esEmergenciaCritica: false, canalOficialContacto: null },
  { nombre: 'Agua y alcantarillado', descripcion: 'Fugas, cortes de agua y alcantarillado colapsado', prioridadBase: 3, esEmergenciaCritica: false, canalOficialContacto: null },
  { nombre: 'Áreas verdes y arbolado', descripcion: 'Plazas, poda y arbolado en riesgo', prioridadBase: 1, esEmergenciaCritica: false, canalOficialContacto: null },
  { nombre: 'Seguridad ciudadana', descripcion: 'Hechos delictuales o situaciones de riesgo', prioridadBase: 4, esEmergenciaCritica: false, canalOficialContacto: 'Carabineros 133' },
  { nombre: 'Accidente de tránsito', descripcion: 'Choques y atropellos con personas afectadas', prioridadBase: 5, esEmergenciaCritica: true, canalOficialContacto: 'SAMU 131' },
  { nombre: 'Incendio', descripcion: 'Incendios estructurales, forestales o pastizales', prioridadBase: 5, esEmergenciaCritica: true, canalOficialContacto: 'Bomberos 132' },
];

async function main() {
  for (const [i, nombre] of estados.entries())
    await prisma.estado.upsert({ where: { nombre }, update: {}, create: { nombre, orden: i + 1 } });
  for (const c of categorias) {
    const existe = await prisma.categoria.findFirst({ where: { nombre: c.nombre } });
    if (!existe) await prisma.categoria.create({ data: c });
  }
  console.log('Seed OK');
}
main().then(() => prisma.$disconnect());
