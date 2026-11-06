
import { useEffect, useRef, useState } from 'react'
import axios from 'axios'
import {
  obtenerUsuarios,
  actualizarRolUsuario,
} from '../services/usuarios.service'
import type {
  UsuarioListado,
  UsuariosPaginationMeta,
  RolUsuario,
} from '../types/usuario'

const LIMITE_USUARIOS = 10

interface UseUsuariosResult {
  usuarios: UsuarioListado[]
  meta: UsuariosPaginationMeta | null
  pagina: number
  cargando: boolean
  error: string | null
  actualizandoRolId: string | null
  mensajeRol: string | null
  errorRol: string | null
  cambiarRol: (
    usuarioId: string,
    nuevoRol: RolUsuario,
  ) => Promise<boolean>
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

  // Issue #148: estados para actualizar roles
  const [actualizandoRolId, setActualizandoRolId] =
    useState<string | null>(null)
  const [mensajeRol, setMensajeRol] = useState<string | null>(null)
  const [errorRol, setErrorRol] = useState<string | null>(null)

  // Evita solicitudes simultáneas y duplicadas
  const actualizandoRef = useRef<string | null>(null)

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
          'message' in err &&
          typeof err.message === 'string'
        ) {
          mensaje = err.message
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

  // Issue #148: cambiar el rol de un usuario
  async function cambiarRol(
    usuarioId: string,
    nuevoRol: RolUsuario,
  ): Promise<boolean> {
    // Impedir peticiones duplicadas
    if (actualizandoRef.current !== null || cargando) {
      return false
    }

    const usuario = usuarios.find(
      (item) => item.id === usuarioId,
    )

    if (!usuario) {
      return false
    }

    // No solicitar un cambio si ya tiene ese rol
    if (
      usuario.roles.length === 1 &&
      usuario.roles[0] === nuevoRol
    ) {
      return false
    }

    actualizandoRef.current = usuarioId
    setActualizandoRolId(usuarioId)
    setMensajeRol(null)
    setErrorRol(null)

    try {
      const resultado = await actualizarRolUsuario(
        usuarioId,
        nuevoRol,
      )

      // Actualizar solo después de la confirmación
      setUsuarios((actuales) =>
        actuales.map((item) =>
          item.id === resultado.id
            ? { ...item, roles: resultado.roles }
            : item,
        ),
      )

      setMensajeRol(
        `El rol de ${usuario.nombre} se actualizó correctamente.`,
      )

      return true
    } catch (err: unknown) {
      let mensaje = 'No fue posible actualizar el rol del usuario.'
      let statusCode: number | undefined

      // Reconocer errores originales de Axios
      if (axios.isAxiosError(err)) {
        statusCode = err.response?.status
      }
      // Reconocer ApiError transformado por el interceptor HTTP
      else if (
        typeof err === 'object' &&
        err !== null &&
        'statusCode' in err &&
        typeof err.statusCode === 'number'
      ) {
        statusCode = err.statusCode
      }

      // Mensajes específicos según el código HTTP
      if (statusCode === 403) {
        mensaje = 'No tienes permisos para cambiar este rol.'
      } else if (statusCode === 404) {
        mensaje = 'El usuario ya no existe.'
      } else if (statusCode === 400) {
        mensaje = 'El rol solicitado no es válido.'
      } else if (
        typeof err === 'object' &&
        err !== null &&
        'message' in err &&
        typeof err.message === 'string' &&
        err.message.trim()
      ) {
        mensaje = err.message
      }

      setErrorRol(mensaje)
      return false
    } finally {
      // Liberar bloqueo incluso cuando ocurre un error
      actualizandoRef.current = null
      setActualizandoRolId(null)
    }
  }

  function siguientePagina() {
    if (
      cargando ||
      actualizandoRef.current !== null ||
      !meta ||
      pagina >= meta.totalPages
    ) {
      return
    }

    setMensajeRol(null)
    setErrorRol(null)
    setPagina((actual) => actual + 1)
  }

  function paginaAnterior() {
    if (
      cargando ||
      actualizandoRef.current !== null ||
      pagina <= 1
    ) {
      return
    }

    setMensajeRol(null)
    setErrorRol(null)
    setPagina((actual) => actual - 1)
  }

  function reintentar() {
    if (actualizandoRef.current !== null) {
      return
    }

    setIntento((actual) => actual + 1)
  }

  return {
    usuarios,
    meta,
    pagina,
    cargando,
    error,
    actualizandoRolId,
    mensajeRol,
    errorRol,
    cambiarRol,
    siguientePagina,
    paginaAnterior,
    reintentar,
  }
}
