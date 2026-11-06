import { useEffect, useState } from 'react'
import { useAuth } from '../contexts/AuthContext'
import {
  obtenerEstadosCiudadano,
  obtenerIncidentesCiudadano,
  type OpcionEstadoCiudadano,
} from '../services/inicioCiudadano.service'
import type { Incidente } from '../types/incidente'

type VistaReportes = 'todos' | 'mis-reportes'

interface ErrorConsulta {
  mensaje: string
  permiteReintentar: boolean
}

interface ResultadoConsulta {
  clave: string
  incidentes: Incidente[]
  estados: OpcionEstadoCiudadano[]
  error: ErrorConsulta | null
}

const POR_PAGINA = 10

function normalizarTexto(valor: string): string {
  return valor
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLocaleLowerCase('es')
    .trim()
}

function interpretarError(error: unknown): ErrorConsulta {
  const codigo =
    typeof error === 'object' &&
    error !== null &&
    'statusCode' in error
      ? Number(error.statusCode)
      : undefined

  if (codigo === 401) {
    return {
      mensaje: 'Tu sesión ya no es válida. Inicia sesión nuevamente.',
      permiteReintentar: false,
    }
  }

  if (codigo === 403) {
    return {
      mensaje: 'No tienes permisos para consultar estos incidentes.',
      permiteReintentar: false,
    }
  }

  if (codigo === 400) {
    return {
      mensaje:
        'El servidor rechazó la consulta. Restablece el filtro de estado.',
      permiteReintentar: false,
    }
  }

  return {
    mensaje: 'No se pudieron cargar los incidentes. Inténtalo nuevamente.',
    permiteReintentar: true,
  }
}

export function useInicioCiudadano() {
  const { user, isAuthenticated, isLoading, isCiudadano } = useAuth()

  const usuarioId = user?.id ?? ''
  const token = localStorage.getItem('access_token')

  const sesionDisponible =
    isAuthenticated &&
    isCiudadano &&
    !isLoading &&
    Boolean(usuarioId) &&
    Boolean(token)

  const claveSesion = JSON.stringify([
    usuarioId,
    token,
    sesionDisponible,
  ])

  const [seleccion, setSeleccion] = useState({
    sesion: claveSesion,
    vista: 'todos' as VistaReportes,
    estadoId: '',
    busqueda: '',
    pagina: 1,
    intento: 0,
  })

  const seleccionActual =
    seleccion.sesion === claveSesion
      ? seleccion
      : {
          sesion: claveSesion,
          vista: 'todos' as VistaReportes,
          estadoId: '',
          busqueda: '',
          pagina: 1,
          intento: 0,
        }

  const { vista, estadoId, busqueda, pagina, intento } = seleccionActual

  const [resultado, setResultado] =
    useState<ResultadoConsulta | null>(null)

  // La búsqueda se aplica localmente a la colección completa recibida.
  // No provoca una consulta por cada letra.
  const claveConsulta = JSON.stringify([
    claveSesion,
    vista,
    estadoId,
    intento,
  ])

  useEffect(() => {
    const controller = new AbortController()

    async function cargar() {
      if (!sesionDisponible) {
        setResultado(null)
        return
      }

      try {
        const consulta = {
          estadoId,
          ...(vista === 'mis-reportes'
            ? { usuarioReportaId: usuarioId }
            : {}),
        }

        const generales = obtenerIncidentesCiudadano(
          {},
          controller.signal,
        )

        const filtrados =
          vista === 'todos' && !estadoId
            ? generales
            : obtenerIncidentesCiudadano(
                consulta,
                controller.signal,
              )

        const [coleccionGeneral, incidentes] = await Promise.all([
          generales,
          filtrados,
        ])

        if (
          controller.signal.aborted ||
          localStorage.getItem('access_token') !== token
        ) {
          return
        }

        setResultado({
          clave: claveConsulta,
          incidentes,
          estados: obtenerEstadosCiudadano(coleccionGeneral),
          error: null,
        })
      } catch (error: unknown) {
        if (
          controller.signal.aborted ||
          localStorage.getItem('access_token') !== token
        ) {
          return
        }

        setResultado({
          clave: claveConsulta,
          incidentes: [],
          estados: [],
          error: interpretarError(error),
        })
      }
    }

    void cargar()

    return () => controller.abort()
  }, [
    sesionDisponible,
    usuarioId,
    token,
    vista,
    estadoId,
    claveConsulta,
  ])

  const resultadoActual =
    sesionDisponible && resultado?.clave === claveConsulta
      ? resultado
      : null

  const cargando =
    isLoading || (sesionDisponible && resultadoActual === null)

  const error = resultadoActual?.error ?? null
  const incidentes = resultadoActual?.incidentes ?? []
  const estados = resultadoActual?.estados ?? []

  const termino = normalizarTexto(busqueda)

  const incidentesFiltrados = termino
    ? incidentes.filter((incidente) => {
        const categoria = normalizarTexto(
          incidente.categoria?.nombre ?? '',
        )
        const comuna = normalizarTexto(
          incidente.ubicacion?.comuna ?? '',
        )

        return categoria.includes(termino) || comuna.includes(termino)
      })
    : incidentes

  const totalResultados = incidentesFiltrados.length
  const totalPaginas = Math.ceil(totalResultados / POR_PAGINA)
  const paginaActual = Math.min(pagina, Math.max(1, totalPaginas))
  const inicio = (paginaActual - 1) * POR_PAGINA

  const incidentesPagina = incidentesFiltrados.slice(
    inicio,
    inicio + POR_PAGINA,
  )

  function cambiarVista(nuevaVista: VistaReportes) {
    setSeleccion({
      ...seleccionActual,
      vista: nuevaVista,
      pagina: 1,
    })
  }

  function cambiarEstado(nuevoEstadoId: string) {
    setSeleccion({
      ...seleccionActual,
      estadoId: nuevoEstadoId,
      pagina: 1,
    })
  }

  function cambiarBusqueda(nuevaBusqueda: string) {
    setSeleccion({
      ...seleccionActual,
      busqueda: nuevaBusqueda,
      pagina: 1,
    })
  }

  function restablecerEstado() {
    cambiarEstado('')
  }

  function reintentar() {
    setSeleccion({
      ...seleccionActual,
      intento: intento + 1,
      pagina: 1,
    })
  }

  function paginaAnterior() {
    if (cargando || error || paginaActual <= 1) return

    setSeleccion({
      ...seleccionActual,
      pagina: paginaActual - 1,
    })
  }

  function paginaSiguiente() {
    if (cargando || error || paginaActual >= totalPaginas) return

    setSeleccion({
      ...seleccionActual,
      pagina: paginaActual + 1,
    })
  }

  return {
    sesionDisponible,
    vista,
    estadoId,
    busqueda,
    estados,
    incidentesFiltrados,
    incidentesPagina,
    cargando,
    error,
    cambiarVista,
    cambiarEstado,
    cambiarBusqueda,
    restablecerEstado,
    reintentar,
    paginaActual,
    totalPaginas,
    totalResultados,
    desde: totalResultados === 0 ? 0 : inicio + 1,
    hasta: Math.min(inicio + POR_PAGINA, totalResultados),
    puedeRetroceder:
      sesionDisponible && !cargando && !error && paginaActual > 1,
    puedeAvanzar:
      sesionDisponible &&
      !cargando &&
      !error &&
      paginaActual < totalPaginas,
    paginaAnterior,
    paginaSiguiente,
  }
}