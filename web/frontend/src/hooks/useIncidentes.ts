import { useEffect, useMemo, useState } from 'react'
import { obtenerIncidentes } from '../services/incidentes.service'
import type {
  FiltrosIncidentes,
  Incidente,
} from '../types/incidente'

interface ErrorConsulta {
  mensaje: string
  permiteReintentar: boolean
}

interface OpcionFiltro {
  id: string
  nombre: string
}

const FILTROS_INICIALES: FiltrosIncidentes = {
  estadoId: '',
  comuna: '',
  categoriaId: '',
}

const POR_PAGINA = 10

function interpretarError(error: unknown): ErrorConsulta {
  const statusCode =
    typeof error === 'object' &&
    error !== null &&
    'statusCode' in error
      ? Number(error.statusCode)
      : undefined

  if (statusCode === 401) {
    return {
      mensaje: 'Tu sesión no es válida. Ingresa nuevamente.',
      permiteReintentar: false,
    }
  }

  if (statusCode === 403) {
    return {
      mensaje: 'No tienes permisos para consultar los incidentes.',
      permiteReintentar: false,
    }
  }

  return {
    mensaje: 'No se pudieron cargar los incidentes. Inténtalo nuevamente.',
    permiteReintentar: true,
  }
}

function obtenerOpciones(
  incidentes: Incidente[],
  campo: 'estado' | 'categoria',
): OpcionFiltro[] {
  const opciones = new Map<string, string>()

  for (const incidente of incidentes) {
    const relacion = incidente[campo]
    const id =
      relacion?.id ??
      (campo === 'estado'
        ? incidente.estadoId
        : incidente.categoriaId)

    const nombre = relacion?.nombre?.trim()

    if (id && nombre) {
      opciones.set(id, nombre)
    }
  }

  return Array.from(opciones, ([id, nombre]) => ({ id, nombre }))
    .sort((a, b) => a.nombre.localeCompare(b.nombre, 'es'))
}

export function useIncidentes() {
  const [filtros, setFiltros] =
    useState<FiltrosIncidentes>(FILTROS_INICIALES)

  const [incidentes, setIncidentes] = useState<Incidente[]>([])
  const [coleccionOpciones, setColeccionOpciones] =
    useState<Incidente[]>([])

  const [cargandoConsulta, setCargandoConsulta] = useState(true)
  const [errorConsulta, setErrorConsulta] =
    useState<ErrorConsulta | null>(null)

  const [pagina, setPagina] = useState(1)
  const [intento, setIntento] = useState(0)
  const [consultaTerminada, setConsultaTerminada] =
    useState<string | null>(null)

  const { estadoId, comuna, categoriaId } = filtros

  const claveConsulta = JSON.stringify([
    estadoId,
    comuna,
    categoriaId,
    intento,
  ])

  useEffect(() => {
    const controller = new AbortController()

    async function cargarIncidentes() {
      setCargandoConsulta(true)
      setErrorConsulta(null)

      try {
        const datos = await obtenerIncidentes(controller.signal, {
          estadoId,
          comuna,
          categoriaId,
        })

        if (controller.signal.aborted) return

        setIncidentes(datos)

        // Las opciones se obtienen de la colección completa sin filtros.
        // Se conservan al filtrar, incluso cuando no hay coincidencias.
        if (!estadoId && !comuna && !categoriaId) {
          setColeccionOpciones(datos)
        }
      } catch (error: unknown) {
        if (controller.signal.aborted) return

        setIncidentes([])
        setErrorConsulta(interpretarError(error))
      } finally {
        if (!controller.signal.aborted) {
          setConsultaTerminada(claveConsulta)
          setCargandoConsulta(false)
        }
      }
    }

    void cargarIncidentes()

    // Una consulta anterior no puede actualizar el estado al cancelarse.
    return () => controller.abort()
  }, [estadoId, comuna, categoriaId, claveConsulta])

  const estados = useMemo(
    () => obtenerOpciones(coleccionOpciones, 'estado'),
    [coleccionOpciones],
  )

  const categorias = useMemo(
    () => obtenerOpciones(coleccionOpciones, 'categoria'),
    [coleccionOpciones],
  )

  const comunas = useMemo(() => {
    const nombres = coleccionOpciones
      .map((incidente) => incidente.ubicacion?.comuna?.trim())
      .filter((nombre): nombre is string => Boolean(nombre))

    return [...new Set(nombres)]
      .sort((a, b) => a.localeCompare(b, 'es'))
  }, [coleccionOpciones])

  const cargando =
    cargandoConsulta || consultaTerminada !== claveConsulta

  const error = cargando ? null : errorConsulta

  const totalResultados =
    cargando || error ? 0 : incidentes.length

  const totalPaginas = Math.ceil(totalResultados / POR_PAGINA)

  const paginaActual = Math.min(
    pagina,
    Math.max(1, totalPaginas),
  )

  const inicio = (paginaActual - 1) * POR_PAGINA

  const incidentesPagina =
    cargando || error
      ? []
      : incidentes.slice(inicio, inicio + POR_PAGINA)

  const hayFiltros = Boolean(estadoId || comuna || categoriaId)

  function cambiarFiltro(
    campo: keyof FiltrosIncidentes,
    valor: string,
  ) {
    setPagina(1)
    setFiltros((actuales) => ({
      ...actuales,
      [campo]: valor,
    }))
  }

  function restablecerFiltros() {
    setPagina(1)
    setFiltros({ ...FILTROS_INICIALES })
  }

  function reintentar() {
    setIntento((actual) => actual + 1)
  }

  function paginaAnterior() {
    if (cargando || error) return
    setPagina(Math.max(1, paginaActual - 1))
  }

  function paginaSiguiente() {
    if (cargando || error) return

    if (paginaActual < totalPaginas) {
      setPagina(paginaActual + 1)
    }
  }

  return {
    // Conservamos esta propiedad para la pantalla existente.
    incidentes,
    incidentesPagina,
    cargando,
    error,
    reintentar,
    filtros,
    estados,
    categorias,
    comunas,
    hayFiltros,
    cambiarFiltro,
    restablecerFiltros,
    paginaActual,
    totalPaginas,
    totalResultados,
    desde: totalResultados === 0 ? 0 : inicio + 1,
    hasta: Math.min(inicio + POR_PAGINA, totalResultados),
    puedeRetroceder:
      !cargando && !error && paginaActual > 1,
    puedeAvanzar:
      !cargando && !error && paginaActual < totalPaginas,
    paginaAnterior,
    paginaSiguiente,
  }
}