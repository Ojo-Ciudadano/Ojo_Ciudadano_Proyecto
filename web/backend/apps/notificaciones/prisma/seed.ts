import { PrismaPg } from '@prisma/adapter-pg';
import { PrismaClient } from '../src/generated/prisma';

const connectionString = process.env.DATABASE_URL_NOTIFICACIONES;
if (!connectionString) {
  throw new Error('DATABASE_URL_NOTIFICACIONES no está definida en web/backend/.env');
}

const adapter = new PrismaPg({ connectionString });
const prisma = new PrismaClient({ adapter });

async function main() {
  const usuarioId = '00000000-0000-4000-8000-000000000001';
  const incidenteId = '20000000-0000-4000-8000-000000000001';

  const seedNotifications = [
    {
      id: '30000000-0000-4000-8000-000000000001',
      usuarioId,
      incidenteId,
      mensaje: 'Tu reporte de incidente fue recibido y se encuentra en revisión.',
      leido: false,
    },
    {
      id: '30000000-0000-4000-8000-000000000002',
      usuarioId,
      incidenteId,
      mensaje: 'El incidente que reportaste cambió de estado a EN_PROCESO.',
      leido: false,
    },
  ];

  for (const notificacion of seedNotifications) {
    const { id, ...data } = notificacion;
    await prisma.notificacion.upsert({
      where: { id },
      update: data,
      create: notificacion,
    });
  }

  console.log(`Notificaciones de prueba aseguradas: ${seedNotifications.length}`);

  const total = await prisma.notificacion.count();
  console.log(`Total de notificaciones en BD: ${total}`);
}

main()
  .catch((error) => {
    console.error(error);
    process.exit(1);
  })
  .finally(() => prisma.$disconnect());