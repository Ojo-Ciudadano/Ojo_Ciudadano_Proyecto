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

/**
 * Consulta los incidentes con filtros opcionales por estado y categoría (#140, #143)
 * y los transforma en marcadores para el mapa.
 *
 * Cancela solicitudes en vuelo cuando el usuario modifica los filtros para
 * evitar que respuestas desactualizadas sobrescriban la selección más reciente.
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

        setIncidentes(datos)
        setColeccionOpciones((actual) =>
          actualizarColeccionCatalogoMapa(
            actual,
            datos,
            !estadoId && !categoriaId,
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