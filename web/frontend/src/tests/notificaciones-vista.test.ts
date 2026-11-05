import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import {
  interpretarFecha,
  mostrarFecha,
  texto,
} from '../utils/notificacionesFormato'
import {
  getInitialRouteForRoles,
  getVisibleNavigation,
  NAVIGATION_RULES,
  ROLES,
} from '../config/navigation'

describe('Formato de notificaciones (#145)', () => {
  it('limpia espacios conservando el contenido del mensaje', () => {
    assert.equal(
      texto('  Tu incidente cambió de estado.  '),
      'Tu incidente cambió de estado.',
    )
  })

  it('tolera textos ausentes o de un tipo incorrecto', () => {
    for (const valor of [undefined, null, 123, {}, [], '', '   ']) {
      assert.equal(texto(valor), '')
    }
  })

  it('interpreta una fecha ISO entregada por el backend', () => {
    const fecha = interpretarFecha('2026-11-04T19:58:00.000Z')

    assert.ok(fecha instanceof Date)
    assert.equal(fecha.toISOString(), '2026-11-04T19:58:00.000Z')
  })

  it('rechaza fechas ausentes o inválidas sin lanzar errores', () => {
    for (const valor of [
      undefined,
      null,
      '',
      '   ',
      'fecha incorrecta',
      '2026-99-99',
      123,
      {},
    ]) {
      assert.equal(interpretarFecha(valor), null)
    }
  })

  it('presenta la fecha con el formato regional configurado', () => {
    const fecha = new Date('2026-11-04T19:58:00.000Z')
    const esperado = new Intl.DateTimeFormat('es-CL', {
      dateStyle: 'medium',
      timeStyle: 'short',
    }).format(fecha)

    assert.equal(mostrarFecha(fecha), esperado)
  })
})

describe('Navegación de notificaciones (#145)', () => {
  it('registra una sola entrada disponible para ambos roles', () => {
    const entradas = NAVIGATION_RULES.filter(
      (regla) => regla.path === '/notificaciones',
    )

    assert.equal(entradas.length, 1)
    assert.equal(entradas[0].label, 'Notificaciones')
    assert.equal(entradas[0].section, 'main')
    assert.deepEqual(
      [...entradas[0].allowedRoles].sort(),
      [ROLES.ADMIN, ROLES.CIUDADANO].sort(),
    )
  })

  it('muestra Notificaciones en el menú de cada rol', () => {
    for (const rol of [ROLES.CIUDADANO, ROLES.ADMIN]) {
      const menu = getVisibleNavigation([rol], 'main')

      assert.ok(
        menu.some((entrada) => entrada.path === '/notificaciones'),
      )
    }
  })

  it('no ofrece el menú a sesiones sin roles reconocidos', () => {
    for (const roles of [undefined, null, [], ['DESCONOCIDO']]) {
      assert.deepEqual(getVisibleNavigation(roles), [])
    }
  })

  it('envía al Ciudadano y al Administrador a sus respectivos inicios', () => {
    assert.equal(getInitialRouteForRoles([ROLES.CIUDADANO]), '/')
    assert.equal(getInitialRouteForRoles([ROLES.ADMIN]), '/dashboard')
    assert.equal(getInitialRouteForRoles([]), '/login')
  })

  it('no ofrece el dashboard al Ciudadano ni Nuevo reporte al Administrador', () => {
    const ciudadano = getVisibleNavigation([ROLES.CIUDADANO], 'main')
    const administrador = getVisibleNavigation([ROLES.ADMIN], 'main')

    assert.equal(
      ciudadano.some((entrada) => entrada.path === '/dashboard'),
      false,
    )

    assert.equal(
      administrador.some(
        (entrada) => entrada.path === '/reportes/nuevo',
      ),
      false,
    )
  })
})