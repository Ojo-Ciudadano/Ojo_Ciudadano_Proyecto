const { PrismaClient } = require('@prisma/client')
const bcrypt = require('bcryptjs')

const prisma = new PrismaClient()

async function main() {
  const email = 'barbara@test.cl'
  const password = 'test123'
  const nombre = 'Barbara'
  const apellido = 'Test'

  // Verificar si ya existe
  const existente = await prisma.usuario.findUnique({ where: { email } })
  if (existente) {
    console.log('El usuario ya existe:', existente.email)
    return
  }

  // Crear roles si no existen
  const rolCiudadano = await prisma.rol.upsert({
    where: { nombre: 'CIUDADANO' },
    update: {},
    create: { nombre: 'CIUDADANO' },
  })

  const rolAdmin = await prisma.rol.upsert({
    where: { nombre: 'ADMINISTRADOR' },
    update: {},
    create: { nombre: 'ADMINISTRADOR' },
  })

  // Crear usuario
  const passwordHash = bcrypt.hashSync(password, 10)
  const usuario = await prisma.usuario.create({
    data: {
      nombre,
      email,
      passwordHash,
      apellido,
      roles: {
        create: [
          { rolId: rolCiudadano.id },
          { rolId: rolAdmin.id },
        ],
      },
    },
  })

  console.log('Usuario creado exitosamente:')
  console.log('  Email:', usuario.email)
  console.log('  Nombre:', usuario.nombre)
  console.log('  Contraseña:', password)
  console.log('  Roles: CIUDADANO, ADMINISTRADOR')
}

main()
  .catch((e) => {
    console.error('Error:', e.message)
    process.exit(1)
  })
  .finally(() => prisma.$disconnect())
