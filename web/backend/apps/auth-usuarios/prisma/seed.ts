import { PrismaPg } from '@prisma/adapter-pg';
import bcrypt from 'bcryptjs';
import { PrismaClient } from '../src/generated/prisma';

const SALT_ROUNDS = 10;

/**
 * Fallback exclusivo para desarrollo local. En cualquier entorno compartido o
 * remoto se debe definir DEMO_CITIZEN_PASSWORD en web/backend/.env.
 */
const DEMO_PASSWORD_FALLBACK = 'CiudadanoDemo123!';

interface CiudadanoDemo {
  id: string;
  nombre: string;
  email: string;
  telefono: string;
}

/**
 * UUIDs fijos y reproducibles (Issue #151). Permiten referenciar a los
 * ciudadanos demo desde pruebas, fixtures o documentación sin consultar la DB.
 */
const CIUDADANOS_DEMO: readonly CiudadanoDemo[] = [
  {
    id: 'a1b2c3d4-0001-4000-8000-000000000001',
    nombre: 'Ciudadano Demo Uno',
    email: 'ciudadano.demo1@ojociudadano.cl',
    telefono: '+56911111111',
  },
  {
    id: 'a1b2c3d4-0002-4000-8000-000000000002',
    nombre: 'Ciudadana Demo Dos',
    email: 'ciudadano.demo2@ojociudadano.cl',
    telefono: '+56922222222',
  },
  {
    id: 'a1b2c3d4-0003-4000-8000-000000000003',
    nombre: 'Ciudadano Demo Tres',
    email: 'ciudadano.demo3@ojociudadano.cl',
    telefono: '+56933333333',
  },
];

const connectionString = process.env.DATABASE_URL_AUTH;
if (!connectionString) {
  throw new Error('DATABASE_URL_AUTH no está definida en web/backend/.env');
}

const adapter = new PrismaPg({ connectionString });
const prisma = new PrismaClient({ adapter });

function hashPassword(password: string): string {
  return bcrypt.hashSync(password, SALT_ROUNDS);
}

function resolveDemoPassword(): string {
  const password = process.env.DEMO_CITIZEN_PASSWORD?.trim();
  if (password) {
    return password;
  }

  console.warn(
    `DEMO_CITIZEN_PASSWORD no está definida; se usa el fallback de desarrollo local (${DEMO_PASSWORD_FALLBACK}).`,
  );
  return DEMO_PASSWORD_FALLBACK;
}

async function upsertRole(nombre: string) {
  return prisma.rol.upsert({
    where: { nombre },
    update: {},
    create: { nombre },
  });
}

async function ensureUsuarioRol(usuarioId: string, rolId: string) {
  await prisma.usuarioRol.upsert({
    where: { usuarioId_rolId: { usuarioId, rolId } },
    update: {},
    create: { usuarioId, rolId },
  });
}

type EstadoCiudadano = 'creado' | 'actualizado' | 'sin-cambios' | 'omitido';

async function asegurarCiudadanoDemo(
  demo: CiudadanoDemo,
  rolId: string,
  password: string,
): Promise<EstadoCiudadano> {
  // Si el correo ya pertenece a un usuario con otro ID, no se toca: no forma
  // parte del conjunto demo y sobrescribirlo violaría la idempotencia segura.
  const existentePorEmail = await prisma.usuario.findUnique({
    where: { email: demo.email },
    select: { id: true },
  });

  if (existentePorEmail && existentePorEmail.id !== demo.id) {
    return 'omitido';
  }

  const existente = await prisma.usuario.findUnique({ where: { id: demo.id } });

  if (!existente) {
    await prisma.usuario.create({
      data: {
        id: demo.id,
        nombre: demo.nombre,
        email: demo.email,
        passwordHash: hashPassword(password),
        telefono: demo.telefono,
      },
    });
    await ensureUsuarioRol(demo.id, rolId);
    return 'creado';
  }

  const passwordCoincide = bcrypt.compareSync(password, existente.passwordHash);
  const requiereActualizacion =
    !passwordCoincide ||
    existente.nombre !== demo.nombre ||
    existente.email !== demo.email ||
    existente.telefono !== demo.telefono;

  if (requiereActualizacion) {
    await prisma.usuario.update({
      where: { id: demo.id },
      data: {
        nombre: demo.nombre,
        email: demo.email,
        // Se conserva el hash vigente si la contraseña sigue siendo válida.
        passwordHash: passwordCoincide ? existente.passwordHash : hashPassword(password),
        telefono: demo.telefono,
      },
    });
  }

  await ensureUsuarioRol(demo.id, rolId);
  return requiereActualizacion ? 'actualizado' : 'sin-cambios';
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

  await ensureUsuarioRol(admin.id, adminRol.id);

  const demoPassword = resolveDemoPassword();
  const resultados: Array<{ demo: CiudadanoDemo; estado: EstadoCiudadano }> = [];

  for (const demo of CIUDADANOS_DEMO) {
    const estado = await asegurarCiudadanoDemo(demo, ciudadanoRol.id, demoPassword);
    resultados.push({ demo, estado });
  }

  console.log(`Roles asegurados: ${[adminRol, ciudadanoRol].map((r) => r.nombre).join(', ')}`);
  console.log(`Administrador inicial: ${admin.email}`);
  console.log(`Ciudadanos demo (rol ${ciudadanoRol.nombre}):`);

  for (const { demo, estado } of resultados) {
    console.log(`  - ${demo.email} | id=${demo.id} | ${estado}`);
  }

  const omitidos = resultados.filter(({ estado }) => estado === 'omitido');
  if (omitidos.length > 0) {
    console.warn(
      `Aviso: ${omitidos.length} correo(s) demo ya existían con otro UUID y no fueron modificados: ${omitidos
        .map(({ demo }) => demo.email)
        .join(', ')}`,
    );
  }
}

main()
  .catch((error) => {
    console.error(error);
    process.exit(1);
  })
  .finally(() => prisma.$disconnect());
