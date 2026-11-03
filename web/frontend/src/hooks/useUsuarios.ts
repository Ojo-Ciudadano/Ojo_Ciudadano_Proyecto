import { useEffect, useState } from 'react'
import axios from 'axios'
import { obtenerUsuarios } from '../services/usuarios.service'
import type {
  UsuarioListado,
  UsuariosPaginationMeta,
} from '../types/usuario'

const LIMITE_USUARIOS = 10

interface UseUsuariosResult {
  usuarios: UsuarioListado[]
  meta: UsuariosPaginationMeta | null
  pagina: number
  cargando: boolean
  error: string | null
  siguientePagina: () => void
  paginaAnterior: () => void
  reintentar: () => void
}

export function useUsuarios(): UseUsuariosResult {
  const [usuarios, setUsuarios] = useState<UsuarioListado[]>([])
  const [meta, setMeta] = useState<UsuariosPaginationMeta | null>(null)

  const [pagina, setPagina] = useState(1)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [intento, setIntento] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    let vigente = true

    async function cargarUsuarios() {
      setCargando(true)
      setError(null)

      try {
        const resultado = await obtenerUsuarios(
          pagina,
          LIMITE_USUARIOS,
          controller.signal,
        )

        if (!vigente || controller.signal.aborted) {
          return
        }

        setUsuarios(resultado.data)
        setMeta(resultado.meta)
      } catch (err: unknown) {
        if (
          !vigente ||
          controller.signal.aborted ||
          axios.isCancel(err)
        ) {
          return
        }

        setUsuarios([])
        setMeta(null)

        let mensaje = 'No fue posible cargar los usuarios.'

        if (err instanceof Error && err.message) {
          mensaje = err.message
        } else if (
          typeof err === 'object' &&
          err !== null &&
          'message' in err
        ) {
          const mensajeError = err.message

          if (typeof mensajeError === 'string') {
            mensaje = mensajeError
          }
        }

        setError(mensaje)
      } finally {
        if (vigente && !controller.signal.aborted) {
          setCargando(false)
        }
      }
    }

    void cargarUsuarios()

    return () => {
      vigente = false
      controller.abort()
    }
  }, [pagina, intento])

  function siguientePagina() {
    if (cargando || !meta || pagina >= meta.totalPages) {
      return
    }

    setPagina((actual) => actual + 1)
  }

  function paginaAnterior() {
    if (cargando || pagina <= 1) {
      return
    }

    setPagina((actual) => actual - 1)
  }

  function reintentar() {
    setIntento((actual) => actual + 1)
  }

  return {
    usuarios,
    meta,
    pagina,
    cargando,
    error,
    siguientePagina,
    paginaAnterior,
    reintentar,
  }
}