import { useEffect, useState } from 'react'
import { obtenerCategoriasReporte } from '../services/nuevoIncidente.service'
import type { CategoriaReporte } from '../types/nuevoIncidente'

interface ResultadoCategorias {
  intento: number
  categorias: CategoriaReporte[]
  error: string | null
}

function mensajeErrorCategorias(error: unknown): string {
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
    return 'No tienes permisos para consultar las categorías.'
  }

  return 'No se pudieron cargar las categorías. Inténtalo nuevamente.'
}

export function useCategoriasReporte() {
  const [intento, setIntento] = useState(0)
  const [resultado, setResultado] =
    useState<ResultadoCategorias | null>(null)

  useEffect(() => {
    const controller = new AbortController()

    async function cargarCategorias() {
      try {
        const categorias = await obtenerCategoriasReporte(
          controller.signal,
        )

        if (controller.signal.aborted) return

        setResultado({
          intento,
          categorias,
          error: null,
        })
      } catch (error: unknown) {
        if (controller.signal.aborted) return

        setResultado({
          intento,
          categorias: [],
          error: mensajeErrorCategorias(error),
        })
      }
    }

    void cargarCategorias()

    return () => controller.abort()
  }, [intento])

  const resultadoActual =
    resultado?.intento === intento ? resultado : null

  function reintentar() {
    setIntento((actual) => actual + 1)
  }

  return {
    categorias: resultadoActual?.categorias ?? [],
    cargando: resultadoActual === null,
    error: resultadoActual?.error ?? null,
    reintentar,
  }
}