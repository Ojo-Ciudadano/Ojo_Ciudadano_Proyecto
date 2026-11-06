import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  normalizarFiltrosMapa,
  obtenerIncidentesParaMapa,
} from '../services/incidentes.service'
import {
  FILTROS_MAPA_INICIALES,
  actualizarColeccionCatalogoMapa,
  crearCoordinadorConsultasMapa,
  interpretarErrorMapa,
  mapearIncidentesAMarcadores,
  obtenerOpcionesFiltroMapa,
  restablecerFiltrosMapa,
  sincronizarIncidenteSeleccionado,
  tieneFiltrosMapaActivos,
  type ErrorMapa,
} from '../components/map/incidentMarkers'
import type { IncidentMapMarker } from '../components/map/mapConfig'
import type {
  FiltrosMapaIncidentes,
  Incidente,
  OpcionCatalogoMapa,
} from '../types/incidente'

export interface IncidentesMapaState {
  marcadores: IncidentMapMarker[]
  cargando: boolean
  error: ErrorMapa | null
  reintentar: () => void
  filtros: FiltrosMapaIncidentes
  estados: OpcionCatalogoMapa[]
  categorias: OpcionCatalogoMapa[]
  hayFiltros: boolean
  cambiarFiltro: (campo: keyof FiltrosMapaIncidentes, valor: string) => void
  restablecerFiltros: () => void
  incidenteSeleccionadoId: string | null
  seleccionarIncidente: (id: string | null) => void
  totalIncidentes: number
  totalGeorreferenciados: number
  totalOmitidos: number
}

export interface EstadoDatosMapa {
  incidentes: Incidente[]
  coleccionOpciones: Incidente[]
  incidenteSeleccionadoId: string | null
}

export interface EstadoDatosMapa {
  incidentes: Incidente[]
  coleccionOpciones: Incidente[]
  incidenteSeleccionadoId: string | null
}

/**
 * Procesa el resultado de una consulta vigente (#143).
 *
 * Si el incidente con popup abierto (`incidenteSeleccionadoId`) deja de estar
 * representado en los marcadores válidos devueltos, limpia el estado
 * dejándolo en `null` para que no se reabra al restablecer filtros.
 */
export function aplicarResultadoConsultaMapa(
  estadoActual: EstadoDatosMapa,
  datos: readonly Incidente[],
  filtros: Partial<FiltrosMapaIncidentes>,
): EstadoDatosMapa {
  const { estadoId, categoriaId } = normalizarFiltrosMapa(filtros)
  const sinFiltrosActivos = !estadoId && !categoriaId
  const nuevosMarcadores = mapearIncidentesAMarcadores(datos)

  return {
    incidentes: [...datos],
    coleccionOpciones: actualizarColeccionCatalogoMapa(
      estadoActual.coleccionOpciones,
      datos,
      sinFiltrosActivos,
    ),
    incidenteSeleccionadoId: sincronizarIncidenteSeleccionado(
      estadoActual.incidenteSeleccionadoId,
      nuevosMarcadores,
    ),
  }
}

/**
 * Controlador de estado del hook `useIncidentesMapa` que ejecuta las mismas
 * transiciones de filtrado, restablecimiento y limpieza de `incidenteSeleccionadoId`.
 */
export function crearControladorIncidentesMapa() {
  const coordinador = crearCoordinadorConsultasMapa()
  let filtros: FiltrosMapaIncidentes = { ...FILTROS_MAPA_INICIALES }
  let incidentes: Incidente[] = []
  let coleccionOpciones: Incidente[] = []
  let incidenteSeleccionadoId: string | null = null
  let cargando = true
  let error: ErrorMapa | null = null

  async function ejecutarConsulta(): Promise<void> {
    const consulta = coordinador.iniciarConsulta()
    const { estadoId, categoriaId } = normalizarFiltrosMapa(filtros)

    cargando = true
    error = null

    try {
      const datos = await obtenerIncidentesParaMapa(consulta.signal, {
        estadoId,
        categoriaId,
      })

      if (!consulta.esVigente()) {
        return
      }

      const siguienteEstado = aplicarResultadoConsultaMapa(
        {
          incidentes,
          coleccionOpciones,
          incidenteSeleccionadoId,
        },
        datos,
        { estadoId, categoriaId },
      )

      incidentes = siguienteEstado.incidentes
      coleccionOpciones = siguienteEstado.coleccionOpciones
      incidenteSeleccionadoId = siguienteEstado.incidenteSeleccionadoId
    } catch (err: unknown) {
      if (!consulta.esVigente()) {
        return
      }

      incidentes = []
      incidenteSeleccionadoId = null
      error = interpretarErrorMapa(err)
    } finally {
      if (consulta.esVigente()) {
        cargando = false
      }
    }
  }

  return {
    cargarInicial(): Promise<void> {
      return ejecutarConsulta()
    },
    cambiarFiltro(
      campo: keyof FiltrosMapaIncidentes,
      valor: string,
    ): Promise<void> {
      filtros = {
        ...filtros,
        [campo]: valor,
      }
      return ejecutarConsulta()
    },
    restablecerFiltros(): Promise<void> {
      filtros = restablecerFiltrosMapa()
      return ejecutarConsulta()
    },
    seleccionarIncidente(id: string | null): void {
      const marcadoresActuales = error
        ? []
        : mapearIncidentesAMarcadores(incidentes)
      incidenteSeleccionadoId = sincronizarIncidenteSeleccionado(
        id,
        marcadoresActuales,
      )
    },
    obtenerEstado() {
      const marcadores = error ? [] : mapearIncidentesAMarcadores(incidentes)
      const totalIncidentes = error ? 0 : incidentes.length
      const totalGeorreferenciados = marcadores.length

      return {
        filtros: { ...filtros },
        marcadores,
        estados: obtenerOpcionesFiltroMapa(coleccionOpciones, 'estado'),
        categorias: obtenerOpcionesFiltroMapa(coleccionOpciones, 'categoria'),
        hayFiltros: tieneFiltrosMapaActivos(filtros),
        cargando,
        error,
        incidenteSeleccionadoId,
        totalIncidentes,
        totalGeorreferenciados,
        totalOmitidos: Math.max(0, totalIncidentes - totalGeorreferenciados),
      }
    },
  }
}

/**
 * Consulta los incidentes con filtros opcionales por estado y categoría (#140, #143)
 * y los transforma en marcadores para el mapa.
 *
 * Cancela solicitudes en vuelo cuando el usuario modifica los filtros para
 * evitar que respuestas desactualizadas sobrescriban la selección más reciente
 * y limpia `incidenteSeleccionadoId` cuando su incidente deja de estar
 * representado en los resultados vigentes.
 */
export function useIncidentesMapa(): IncidentesMapaState {
  const [filtros, setFiltros] = useState<FiltrosMapaIncidentes>(
    FILTROS_MAPA_INICIALES,
  )
  const [incidentes, setIncidentes] = useState<Incidente[]>([])
  const [coleccionOpciones, setColeccionOpciones] = useState<Incidente[]>([])
  const [incidenteSeleccionadoId, setIncidenteSeleccionadoId] = useState<
    string | null
  >(null)

  const [cargandoConsulta, setCargandoConsulta] = useState(true)
  const [errorConsulta, setErrorConsulta] = useState<ErrorMapa | null>(null)
  const [intento, setIntento] = useState(0)
  const [consultaTerminada, setConsultaTerminada] = useState<string | null>(
    null,
  )

  const coordinadorRef = useRef(crearCoordinadorConsultasMapa())

  const { estadoId, categoriaId } = normalizarFiltrosMapa(filtros)
  const hayFiltros = tieneFiltrosMapaActivos({ estadoId, categoriaId })

  const claveConsulta = JSON.stringify([estadoId, categoriaId, intento])

  useEffect(() => {
    const consulta = coordinadorRef.current.iniciarConsulta()

    async function cargarIncidentes() {
      setCargandoConsulta(true)
      setErrorConsulta(null)

      try {
        const datos = await obtenerIncidentesParaMapa(consulta.signal, {
          estadoId,
          categoriaId,
        })

        if (!consulta.esVigente()) {
          return
        }

        const nuevosMarcadores = mapearIncidentesAMarcadores(datos)

        setIncidentes(datos)
        setColeccionOpciones((actual) =>
          actualizarColeccionCatalogoMapa(
            actual,
            datos,
            !estadoId && !categoriaId,
          ),
        )
        setIncidenteSeleccionadoId((seleccionActual) =>
          sincronizarIncidenteSeleccionado(
            seleccionActual,
            nuevosMarcadores,
          ),
        )
      } catch (err: unknown) {
        if (!consulta.esVigente()) {
          return
        }

        setIncidentes([])
        setIncidenteSeleccionadoId(null)
        setErrorConsulta(interpretarErrorMapa(err))
      } finally {
        if (consulta.esVigente()) {
          setConsultaTerminada(claveConsulta)
          setCargandoConsulta(false)
        }
      }
    }

    void cargarIncidentes()

    return () => {
      consulta.cancelar()
    }
  }, [estadoId, categoriaId, claveConsulta])

  const estados = useMemo(
    () => obtenerOpcionesFiltroMapa(coleccionOpciones, 'estado'),
    [coleccionOpciones],
  )

  const categorias = useMemo(
    () => obtenerOpcionesFiltroMapa(coleccionOpciones, 'categoria'),
    [coleccionOpciones],
  )

  const cargando = cargandoConsulta || consultaTerminada !== claveConsulta
  const error = cargando ? null : errorConsulta

  const marcadores = useMemo(
    () => (error ? [] : mapearIncidentesAMarcadores(incidentes)),
    [incidentes, error],
  )

  const incidenteSeleccionadoVigente = useMemo(
    () => sincronizarIncidenteSeleccionado(incidenteSeleccionadoId, marcadores),
    [incidenteSeleccionadoId, marcadores],
  )

  const cambiarFiltro = useCallback(
    (campo: keyof FiltrosMapaIncidentes, valor: string) => {
      setFiltros((actuales) => ({
        ...actuales,
        [campo]: valor,
      }))
    },
    [],
  )

  const restablecerFiltros = useCallback(() => {
    setFiltros(restablecerFiltrosMapa())
  }, [])

  const reintentar = useCallback(() => {
    setIntento((actual) => actual + 1)
  }, [])

  const seleccionarIncidente = useCallback((id: string | null) => {
    setIncidenteSeleccionadoId(id)
  }, [])

  const totalIncidentes = error ? 0 : incidentes.length
  const totalGeorreferenciados = marcadores.length

  return {
    marcadores,
    cargando,
    error,
    reintentar,
    filtros,
    estados,
    categorias,
    hayFiltros,
    cambiarFiltro,
    restablecerFiltros,
    incidenteSeleccionadoId: incidenteSeleccionadoVigente,
    seleccionarIncidente,
    totalIncidentes,
    totalGeorreferenciados,
    totalOmitidos: Math.max(0, totalIncidentes - totalGeorreferenciados),
  }
}