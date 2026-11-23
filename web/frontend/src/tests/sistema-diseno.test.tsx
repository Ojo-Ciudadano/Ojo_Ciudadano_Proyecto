import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { afterEach, describe, it } from 'node:test'
import { createElement } from 'react'
import TestRenderer, { act } from 'react-test-renderer'
import { EstadoIncidenteBadge } from '../components/ui/EstadoIncidenteBadge'
import { EstadoVistaMensaje } from '../components/ui/EstadoVistaMensaje'
import {
  obtenerTextoEstado,
  obtenerTextoPrioridad,
  obtenerVarianteEstado,
  obtenerVariantePrioridad,
} from '../components/ui/incidenteIndicadores.utils'
import { PrioridadIncidenteBadge } from '../components/ui/PrioridadIncidenteBadge'

let renderer: TestRenderer.ReactTestRenderer | undefined

afterEach(async () => {
  await act(async () => {
    renderer?.unmount()
  })

  renderer = undefined
})

describe('Sistema de diseño — tokens globales en index.css (#342)', () => {
  it('centraliza variables de paleta, tipografía, foco, estados y prioridades', () => {
    const rutaCss = resolve(process.cwd(), 'src/index.css')
    const css = readFileSync(rutaCss, 'utf8')

    for (const token of [
      '--color-primary:',
      '--color-accent:',
      '--color-danger:',
      '--color-warning:',
      '--font-sans:',
      '--text-base:',
      '--focus-ring:',
      '--status-nuevo-bg:',
      '--status-atencion-bg:',
      '--status-cerrado-bg:',
      '--priority-critica:',
      '--priority-baja:',
      '.ui-btn',
      '.ui-control',
      '.ui-modal',
      '.ui-status-badge',
      '.ui-priority-indicator',
      '.ui-state-box',
    ]) {
      assert.ok(
        css.includes(token),
        `Se esperaba el token o clase compartida "${token}" en src/index.css`,
      )
    }
  })
})

describe('Indicadores unificados de estado y prioridad (#342)', () => {
  it('asigna la misma variante visual a cada estado canónico y sus variantes de escritura', () => {
    assert.equal(obtenerVarianteEstado('Nuevo'), 'nuevo')
    assert.equal(obtenerVarianteEstado('En revisión'), 'nuevo')
    assert.equal(obtenerVarianteEstado('en_revision'), 'nuevo')
    assert.equal(obtenerVarianteEstado('Validado'), 'asignado')
    assert.equal(obtenerVarianteEstado('Asignado'), 'asignado')
    assert.equal(obtenerVarianteEstado('En atención'), 'atencion')
    assert.equal(obtenerVarianteEstado('Resuelto'), 'cerrado')
    assert.equal(obtenerVarianteEstado('Cerrado'), 'cerrado')
    assert.equal(obtenerVarianteEstado('Rechazado'), 'rechazado')
    assert.equal(obtenerVarianteEstado(null), 'otro')

    assert.equal(obtenerTextoEstado('  En revisión  '), 'En revisión')
    assert.equal(obtenerTextoEstado(''), 'Sin información')
  })

  it('asigna variante y texto coherentes a los 5 niveles de prioridad y ausencia de prioridad', () => {
    assert.equal(obtenerVariantePrioridad(5), 'critica')
    assert.equal(obtenerVariantePrioridad(4), 'alta')
    assert.equal(obtenerVariantePrioridad(3), 'media')
    assert.equal(obtenerVariantePrioridad(2), 'moderada')
    assert.equal(obtenerVariantePrioridad(1), 'baja')
    assert.equal(obtenerVariantePrioridad(null), 'desconocida')

    assert.equal(obtenerTextoPrioridad(5), 'Nivel 5')
    assert.equal(obtenerTextoPrioridad(null), 'Sin prioridad')
  })

  it('renderiza EstadoIncidenteBadge y PrioridadIncidenteBadge con clases compartidas', async () => {
    await act(async () => {
      renderer = TestRenderer.create(
        createElement(
          'div',
          null,
          createElement(EstadoIncidenteBadge, { nombre: 'En atención' }),
          createElement(PrioridadIncidenteBadge, { nivel: 4 }),
        ),
      )
    })

    const badgeEstado = renderer!.root.findByProps({
      'data-estado-variante': 'atencion',
    })
    assert.match(
      String(badgeEstado.props.className),
      /ui-status-badge ui-status-badge--atencion/,
    )
    assert.equal(badgeEstado.children.join(''), 'En atención')

    const badgePrioridad = renderer!.root.findByProps({
      'data-prioridad-variante': 'alta',
    })
    assert.match(
      String(badgePrioridad.props.className),
      /ui-priority-indicator ui-priority-indicator--alta/,
    )
    assert.match(JSON.stringify(renderer!.toJSON()), /Nivel 4/)
  })
})

describe('EstadoVistaMensaje compartido (#342)', () => {
  it('renderiza estado de carga con spinner y role="status"', async () => {
    await act(async () => {
      renderer = TestRenderer.create(
        createElement(EstadoVistaMensaje, {
          tipo: 'loading',
          mensaje: 'Cargando incidentes…',
        }),
      )
    })

    const caja = renderer!.root.findByProps({ role: 'status' })
    assert.match(
      String(caja.props.className),
      /ui-state-box ui-state-box--loading/,
    )
    assert.equal(
      renderer!.root.findAllByProps({ className: 'ui-spinner' }).length,
      1,
    )
    assert.match(JSON.stringify(renderer!.toJSON()), /Cargando incidentes…/)
  })

  it('renderiza estado vacío y estado de error conservando mensajes y acciones propias', async () => {
    let reintentos = 0

    await act(async () => {
      renderer = TestRenderer.create(
        createElement(
          EstadoVistaMensaje,
          {
            tipo: 'error',
            titulo: 'No se pudieron cargar las notificaciones',
            mensaje: 'Error temporal de conexión.',
          },
          createElement(
            'button',
            {
              type: 'button',
              className: 'ui-btn ui-btn--danger',
              onClick: () => {
                reintentos += 1
              },
            },
            'Reintentar',
          ),
        ),
      )
    })

    const alerta = renderer!.root.findByProps({ role: 'alert' })
    assert.match(
      String(alerta.props.className),
      /ui-state-box ui-state-box--error/,
    )
    assert.equal(
      renderer!.root.findAllByProps({ className: 'ui-spinner' }).length,
      0,
    )

    const boton = renderer!.root.findByType('button')
    await act(async () => {
      boton.props.onClick()
    })

    assert.equal(reintentos, 1)
  })
})
