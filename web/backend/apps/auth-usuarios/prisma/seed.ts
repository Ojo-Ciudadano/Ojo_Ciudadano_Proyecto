import { PrismaPg } from '@prisma/adapter-pg';
import bcrypt from 'bcryptjs';
import { PrismaClient } from '../src/generated/prisma';

const connectionString = process.env.DATABASE_URL_AUTH;
if (!connectionString) {
  throw new Error('DATABASE_URL_AUTH no está definida en web/backend/.env');
}

const adapter = new PrismaPg({ connectionString });
const prisma = new PrismaClient({ adapter });

function hashPassword(password: string): string {
  return bcrypt.hashSync(password, 10);
}

async function upsertRole(nombre: string) {
  return prisma.rol.upsert({
    where: { nombre },
    update: {},
    create: { nombre },
  });
}

async function main() {
  const [adminRol, ciudadanoRol] = await Promise.all([
    upsertRole('ADMIN'),
    upsertRole('CIUDADANO'),
  ]);

  const adminEmail = 'admin@ojociudadano.cl';
  const adminPasswordHash = hashPassword('Admin123!');
  const admin = await prisma.usuario.upsert({
    where: { email: adminEmail },
    update: { passwordHash: adminPasswordHash },
    create: {
      nombre: 'Administrador',
      email: adminEmail,
      passwordHash: adminPasswordHash,
    },
  });

  await prisma.usuarioRol.upsert({
    where: { usuarioId_rolId: { usuarioId: admin.id, rolId: adminRol.id } },
    update: {},
    create: { usuarioId: admin.id, rolId: adminRol.id },
  });

  console.log(`Roles asegurados: ${[adminRol, ciudadanoRol].map((r) => r.nombre).join(', ')}`);
  console.log(`Administrador inicial: ${admin.email}`);
}

main()
  .catch((error) => {
    console.error(error);
    process.exit(1);
  })
  .finally(() => prisma.$disconnect());