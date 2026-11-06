import { useEffect, useState } from 'react'
import {
  obtenerCandidatosResponsables,
  type PaginaCandidatos,
} from '../services/usuarios.service'

interface ResultadoCandidatos {
  clave: string
  datos: PaginaCandidatos | null
  error: string | null
}

function mensajeError(error: unknown): string {
  const codigo =
    typeof error === 'object' &&
    error !== null &&
    'statusCode' in error
      ? Number(error.statusCode)
      : undefined

  if (codigo === 401) {
    return 'Tu sesión no es válida. Inicia sesión nuevamente.'
  }

  if (codigo === 403) {
    return 'No tienes permisos para consultar los responsables.'
  }

  return 'No se pudieron cargar los responsables. Inténtalo nuevamente.'
}

export function useCandidatosResponsables() {
  const [pagina, setPagina] = useState(1)
  const [intento, setIntento] = useState(0)
  const [resultado, setResultado] =
    useState<ResultadoCandidatos | null>(null)

  const claveConsulta = JSON.stringify([pagina, intento])

  useEffect(() => {
    const controller = new AbortController()

    async function cargarCandidatos() {
      try {
        const datos = await obtenerCandidatosResponsables(
          pagina,
          controller.signal,
        )

        if (controller.signal.aborted) return

        // Si disminuyó el número de páginas, consultar la última válida.
        if (pagina > Math.max(1, datos.meta.totalPages)) {
          setPagina(Math.max(1, datos.meta.totalPages))
          return
        }

        setResultado({
          clave: claveConsulta,
          datos,
          error: null,
        })
      } catch (error: unknown) {
        if (controller.signal.aborted) return

        setResultado({
          clave: claveConsulta,
          datos: null,
          error: mensajeError(error),
        })
      }
    }

    void cargarCandidatos()

    return () => controller.abort()
  }, [pagina, claveConsulta])

  const resultadoActual =
    resultado?.clave === claveConsulta ? resultado : null

  const cargando = resultadoActual === null
  const datos = resultadoActual?.datos ?? null
  const totalPaginas = datos?.meta.totalPages ?? 0

  const puedeRetroceder = !cargando && pagina > 1

  const puedeAvanzar =
    !cargando && datos !== null && pagina < totalPaginas

  function paginaAnterior() {
    if (puedeRetroceder) {
      setPagina((actual) => Math.max(1, actual - 1))
    }
  }

  function paginaSiguiente() {
    if (puedeAvanzar) {
      setPagina((actual) => actual + 1)
    }
  }

  function actualizarCandidatos() {
    setIntento((actual) => actual + 1)
  }

  return {
    candidatos: datos?.data ?? [],
    cargando,
    error: resultadoActual?.error ?? null,
    pagina,
    totalPaginas,
    totalCandidatos: datos?.meta.total ?? 0,
    puedeRetroceder,
    puedeAvanzar,
    paginaAnterior,
    paginaSiguiente,
    actualizarCandidatos,
  }
}