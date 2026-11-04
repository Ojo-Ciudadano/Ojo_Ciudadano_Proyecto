import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import TestRenderer, { act } from 'react-test-renderer'
import { apiClient } from '../services/api'
import {
  normalizarHistorialIncidente,
  obtenerHistorialIncidente,
} from '../services/incidentes.service'
import {
  HistorialIncidente,
  clasificarMovimiento,
  deduplicarManteniendoOrden,
} from '../pages/incidentes/HistorialIncidente'
import type { RegistroHistorialIncidente } from '../types/incidenteDetalle'

const INCIDENTE_A = 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13'
const INCIDENTE_B = 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a99'

const REGISTROS_BASE: RegistroHistorialIncidente[] = [
  {
    id: 'hist-1',
    incidenteId: INCIDENTE_A,
    estado: { id: 'est-1', nombre: 'Nuevo', orden: 1 },
    usuarioId: 'usr-ciudadano-1',
    observacion: 'Reporte creado',
    fecha: '2026-10-29T10:00:00.000Z',
  },
  {
    id: 'hist-2',
    incidenteId: INCIDENTE_A,
    estado: { id: 'est-1', nombre: 'Nuevo', orden: 1 },
    usuarioId: 'usr-admin-1',
    observacion: 'Asignado a usr-admin-2',
    fecha: '2026-10-29T11:00:00.000Z',
  },
  {
    id: 'hist-3',
    incidenteId: INCIDENTE_A,
    estado: { id: 'est-2', nombre: 'En revisión', orden: 2 },
    usuarioId: 'usr-admin-2',
    observacion: null,
    fecha: '2026-10-29T12:00:00.000Z',
  },
]

function obtenerTexto(renderer: TestRenderer.ReactTestRenderer): string {
  return JSON.stringify(renderer.toJSON())
}

describe('Normalización y servicio de historial de incidentes (#149)', () => {
  it('valida arreglos de historial y verifica pertenencia al incidente', () => {
    const normalizado = normalizarHistorialIncidente(
      REGISTROS_BASE,
      INCIDENTE_A,
    )
    assert.equal(normalizado?.length, 3)

    assert.equal(
      normalizarHistorialIncidente({ invalido: true }, INCIDENTE_A),
      null,
    )
    assert.equal(
      normalizarHistorialIncidente(REGISTROS_BASE, INCIDENTE_B),
      null,
    )
  })

  it('clasifica cambios de estado, asignaciones y reasignaciones (#123)', () => {
    assert.equal(clasificarMovimiento(REGISTROS_BASE[0]), 'cambio-estado')
    assert.equal(clasificarMovimiento(REGISTROS_BASE[1]), 'asignacion')
    assert.equal(
      clasificarMovimiento({
        ...REGISTROS_BASE[1],
        observacion: 'Reasignado de usr-admin-2 a usr-admin-3',
      }),
      'reasignacion',
    )
  })

  it('mantiene el orden cronológico del backend y deduplica por id', () => {
    const resultado = deduplicarManteniendoOrden([
      REGISTROS_BASE[0],
      REGISTROS_BASE[1],
      REGISTROS_BASE[0],
      REGISTROS_BASE[2],
    ])

    assert.deepEqual(
      resultado.map((r) => r.id),
      ['hist-1', 'hist-2', 'hist-3'],
    )
  })

  it('consulta GET /incidentes/:id/historial a través del cliente HTTP', async () => {
    const getOriginal = apiClient.get
    let urlLlamada = ''

    apiClient.get = (async (url: string) => {
      urlLlamada = url
      return { data: REGISTROS_BASE }
    }) as typeof apiClient.get

    try {
      const resultado = await obtenerHistorialIncidente(INCIDENTE_A)
      assert.equal(urlLlamada, `/incidentes/${INCIDENTE_A}/historial`)
      assert.equal(resultado.length, 3)
    } finally {
      apiClient.get = getOriginal
    }
  })
})

describe('Componente HistorialIncidente (#149)', () => {
  it('vuelve a consultar el historial cuando cambia la versión tras una mutación', async () => {
    const getOriginal = apiClient.get
    let consultas = 0

    apiClient.get = (async () => {
      consultas += 1
      return { data: REGISTROS_BASE }
    }) as typeof apiClient.get

    try {
      let renderer!: TestRenderer.ReactTestRenderer

      await act(async () => {
        renderer = TestRenderer.create(
          <HistorialIncidente
            incidenteId={INCIDENTE_A}
            historialIncluido={null}
            versionHistorial={1}
          />,
        )
      })
      assert.equal(consultas, 1)

      await act(async () => {
        renderer.update(
          <HistorialIncidente
            incidenteId={INCIDENTE_A}
            historialIncluido={null}
            versionHistorial={2}
          />,
        )
      })
      assert.equal(consultas, 2)

      await act(async () => {
        renderer.update(
          <HistorialIncidente
            incidenteId={INCIDENTE_A}
            historialIncluido={null}
            versionHistorial={2}
          />,
        )
      })
      assert.equal(consultas, 2)
    } finally {
      apiClient.get = getOriginal
    }
  })

  it('reutiliza el historial incluido en el detalle (#125) sin hacer otra petición HTTP', async () => {
    const getOriginal = apiClient.get
    let llamadasHttp = 0

    apiClient.get = (async () => {
      llamadasHttp += 1
      return { data: [] }
    }) as typeof apiClient.get

    try {
      let renderer!: TestRenderer.ReactTestRenderer

      await act(async () => {
        renderer = TestRenderer.create(
          <HistorialIncidente
            incidenteId={INCIDENTE_A}
            historialIncluido={REGISTROS_BASE}
          />,
        )
      })

      assert.equal(llamadasHttp, 0)
      const texto = obtenerTexto(renderer)
      assert.match(texto, /Reporte creado/)
      assert.match(texto, /Asignación de responsable/)
      assert.match(texto, /Sin observación registrada\./)
      assert.match(texto, /usr-ciudadano-1/)
    } finally {
      apiClient.get = getOriginal
    }
  })

  it('muestra un mensaje claro cuando el historial está vacío', async () => {
    let renderer!: TestRenderer.ReactTestRenderer

    await act(async () => {
      renderer = TestRenderer.create(
        <HistorialIncidente
          incidenteId={INCIDENTE_A}
          historialIncluido={[]}
        />,
      )
    })

    const texto = obtenerTexto(renderer)
    assert.match(
      texto,
      /Este incidente aún no registra movimientos en su historial\./,
    )
  })

  it('consulta el endpoint independiente cuando no viene historial, muestra error y permite reintentar', async () => {
    const getOriginal = apiClient.get
    let intentos = 0

    apiClient.get = (async () => {
      intentos += 1
      if (intentos === 1) {
        const error = new Error('Fallo temporal de red') as Error & {
          statusCode?: number
        }
        error.statusCode = 503
        throw error
      }
      return { data: REGISTROS_BASE }
    }) as typeof apiClient.get

    try {
      let renderer!: TestRenderer.ReactTestRenderer

      await act(async () => {
        renderer = TestRenderer.create(
          <HistorialIncidente
            incidenteId={INCIDENTE_A}
            historialIncluido={null}
          />,
        )
      })

      assert.equal(intentos, 1)
      assert.match(
        obtenerTexto(renderer),
        /No se pudo cargar el historial del incidente/,
      )

      const botonReintentar = renderer.root.findByType('button')

      await act(async () => {
        botonReintentar.props.onClick()
      })

      assert.equal(intentos, 2)
      assert.match(obtenerTexto(renderer), /Reporte creado/)
    } finally {
      apiClient.get = getOriginal
    }
  })

  it('no conserva registros del incidente anterior al cambiar de incidente', async () => {
    const getOriginal = apiClient.get

    apiClient.get = (async () => ({
      data: [
        {
          id: 'hist-b1',
          incidenteId: INCIDENTE_B,
          estado: { id: 'est-1', nombre: 'Nuevo', orden: 1 },
          usuarioId: 'usr-b',
          observacion: 'Observación exclusiva del incidente B',
          fecha: '2026-10-30T10:00:00.000Z',
        },
      ],
    })) as typeof apiClient.get

    try {
      let renderer!: TestRenderer.ReactTestRenderer

      await act(async () => {
        renderer = TestRenderer.create(
          <HistorialIncidente
            incidenteId={INCIDENTE_A}
            historialIncluido={REGISTROS_BASE}
          />,
        )
      })

      assert.match(obtenerTexto(renderer), /Reporte creado/)

      await act(async () => {
        renderer.update(
          <HistorialIncidente
            incidenteId={INCIDENTE_B}
            historialIncluido={null}
          />,
        )
      })

      const textoNuevo = obtenerTexto(renderer)
      assert.doesNotMatch(textoNuevo, /Reporte creado/)
      assert.match(textoNuevo, /Observación exclusiva del incidente B/)
    } finally {
      apiClient.get = getOriginal
    }
  })
})