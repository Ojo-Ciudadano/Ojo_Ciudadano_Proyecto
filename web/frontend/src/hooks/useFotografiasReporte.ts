import { useEffect, useRef, useState } from 'react'
import {
  mensajeErrorFotografia,
  subirFotografiaReporte,
} from '../services/fotografiasReporte.service'
import {
  validarFotografia,
  type FotografiaReporte,
} from '../types/fotografiaReporte'

export function useFotografiasReporte() {
  const [fotografias, setFotografias] = useState<FotografiaReporte[]>([])
  const [erroresSeleccion, setErroresSeleccion] = useState<string[]>([])
  const [subiendo, setSubiendo] = useState(false)

  const fotografiasRef = useRef<FotografiaReporte[]>([])
  const seleccionBloqueada = useRef(false)
  const subidaActiva = useRef(false)
  const incidenteAsociado = useRef<string | null>(null)
  const montado = useRef(true)

  useEffect(() => {
    montado.current = true

    return () => {
      montado.current = false

      for (const fotografia of fotografiasRef.current) {
        URL.revokeObjectURL(fotografia.urlPrevisualizacion)
      }

      fotografiasRef.current = []
    }
  }, [])

  function actualizarFotografias(nuevas: FotografiaReporte[]) {
    fotografiasRef.current = nuevas

    if (montado.current) {
      setFotografias(nuevas)
    }
  }

  function bloquearSeleccion() {
    seleccionBloqueada.current = true
  }

  function desbloquearSeleccion() {
    if (!incidenteAsociado.current && !subidaActiva.current) {
      seleccionBloqueada.current = false
    }
  }

  function agregarFotografias(archivos: FileList | File[]) {
    if (seleccionBloqueada.current || subidaActiva.current) return

    const nuevas: FotografiaReporte[] = []
    const errores: string[] = []

    for (const archivo of Array.from(archivos)) {
      const errorValidacion = validarFotografia(archivo)

      if (errorValidacion) {
        errores.push(`${archivo.name}: ${errorValidacion}`)
        continue
      }

      try {
        const fotografia: FotografiaReporte = {
          id: crypto.randomUUID(),
          archivo,
          urlPrevisualizacion: URL.createObjectURL(archivo),
          estado: 'pendiente',
          error: null,
        }

        nuevas.push(fotografia)
      } catch {
        errores.push(
          `${archivo.name}: no se pudo preparar la previsualización.`,
        )
      }
    }

    setErroresSeleccion(errores)

    actualizarFotografias([
      ...fotografiasRef.current,
      ...nuevas,
    ])
  }

  function quitarFotografia(id: string) {
    if (seleccionBloqueada.current || subidaActiva.current) return

    const fotografia = fotografiasRef.current.find(
      (actual) => actual.id === id,
    )

    if (!fotografia) return

    URL.revokeObjectURL(fotografia.urlPrevisualizacion)

    actualizarFotografias(
      fotografiasRef.current.filter((actual) => actual.id !== id),
    )
  }

  function actualizarEstado(
    id: string,
    cambios: Partial<Pick<FotografiaReporte, 'estado' | 'error'>>,
  ) {
    actualizarFotografias(
      fotografiasRef.current.map((fotografia) =>
        fotografia.id === id
          ? { ...fotografia, ...cambios }
          : fotografia,
      ),
    )
  }

  async function subirPendientes(incidenteId: string) {
    const identificador = incidenteId.trim()

    if (!identificador) {
      throw new Error('Falta el identificador del incidente creado.')
    }

    if (subidaActiva.current) return null

    if (
      incidenteAsociado.current &&
      incidenteAsociado.current !== identificador
    ) {
      throw new Error(
        'Las fotografías ya están asociadas a otro incidente.',
      )
    }

    incidenteAsociado.current = identificador
    seleccionBloqueada.current = true
    subidaActiva.current = true
    setSubiendo(true)

    // Las fotografías confirmadas quedan fuera de los reintentos.
    const pendientes = fotografiasRef.current.filter(
      (fotografia) =>
        fotografia.estado === 'pendiente' ||
        fotografia.estado === 'fallida',
    )

    try {
      for (const fotografia of pendientes) {
        if (!montado.current) return null

        actualizarEstado(fotografia.id, {
          estado: 'subiendo',
          error: null,
        })

        try {
          await subirFotografiaReporte(
            identificador,
            fotografia.archivo,
          )

          if (!montado.current) return null

          actualizarEstado(fotografia.id, {
            estado: 'confirmada',
            error: null,
          })
        } catch (error: unknown) {
          if (!montado.current) return null

          actualizarEstado(fotografia.id, {
            estado: 'fallida',
            error: mensajeErrorFotografia(error),
          })
        }
      }

      const actuales = fotografiasRef.current

      return {
        total: actuales.length,
        confirmadas: actuales.filter(
          (fotografia) => fotografia.estado === 'confirmada',
        ).length,
        fallidas: actuales.filter(
          (fotografia) => fotografia.estado === 'fallida',
        ).length,
      }
    } finally {
      subidaActiva.current = false

      if (montado.current) {
        setSubiendo(false)
      }
    }
  }

  function limpiarFotografias() {
    if (subidaActiva.current) return

    for (const fotografia of fotografiasRef.current) {
      URL.revokeObjectURL(fotografia.urlPrevisualizacion)
    }

    actualizarFotografias([])
    setErroresSeleccion([])

    // Si ya existe un incidente, la selección permanece cerrada.
    seleccionBloqueada.current = incidenteAsociado.current !== null
  }

  return {
    fotografias,
    erroresSeleccion,
    subiendo,
    agregarFotografias,
    quitarFotografia,
    bloquearSeleccion,
    desbloquearSeleccion,
    subirPendientes,
    limpiarFotografias,
  }
}