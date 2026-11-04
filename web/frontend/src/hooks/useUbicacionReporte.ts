import { useEffect, useRef, useState } from 'react'
import {
  sonCoordenadasValidas,
  type CoordenadasReporte,
} from '../types/nuevoIncidente'

function mensajeErrorUbicacion(error: GeolocationPositionError): string {
  switch (error.code) {
    case 1:
      return 'No permitiste el acceso a tu ubicación. Habilita el permiso en el navegador y vuelve a intentarlo.'
    case 2:
      return 'No se pudo determinar tu ubicación. Comprueba que la ubicación del dispositivo esté activada y vuelve a intentarlo.'
    case 3:
      return 'Se agotó el tiempo para obtener tu ubicación. Vuelve a intentarlo.'
    default:
      return 'No se pudo obtener tu ubicación. Vuelve a intentarlo.'
  }
}

export function useUbicacionReporte() {
  const [coordenadas, setCoordenadas] =
    useState<CoordenadasReporte | null>(null)
  const [obteniendo, setObteniendo] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const solicitudActiva = useRef(false)
  const numeroSolicitud = useRef(0)

  useEffect(() => {
    return () => {
      numeroSolicitud.current += 1
      solicitudActiva.current = false
    }
  }, [])

  function solicitarUbicacion() {
    if (solicitudActiva.current) return

    setError(null)
    setCoordenadas(null)

    if (!window.isSecureContext) {
      setError(
        'La ubicación requiere una conexión segura HTTPS. Para desarrollo local, utiliza localhost.',
      )
      return
    }

    if (!navigator.geolocation) {
      setError(
        'Este navegador no permite obtener tu ubicación. Utiliza un navegador compatible.',
      )
      return
    }

    solicitudActiva.current = true
    const solicitud = ++numeroSolicitud.current
    setObteniendo(true)

    try {
      navigator.geolocation.getCurrentPosition(
        (posicion) => {
          if (solicitud !== numeroSolicitud.current) return

          const ubicacion: CoordenadasReporte = {
            latitud: posicion.coords.latitude,
            longitud: posicion.coords.longitude,
          }

          if (sonCoordenadasValidas(ubicacion)) {
            setCoordenadas(ubicacion)
            setError(null)
          } else {
            setCoordenadas(null)
            setError(
              'El navegador devolvió coordenadas inválidas. Solicita nuevamente tu ubicación.',
            )
          }

          solicitudActiva.current = false
          setObteniendo(false)
        },
        (fallo) => {
          if (solicitud !== numeroSolicitud.current) return

          setCoordenadas(null)
          setError(mensajeErrorUbicacion(fallo))
          solicitudActiva.current = false
          setObteniendo(false)
        },
        {
          enableHighAccuracy: true,
          timeout: 20000,
          maximumAge: 0,
        },
      )
    } catch {
      if (solicitud !== numeroSolicitud.current) return

      setCoordenadas(null)
      setError(
        'No se pudo solicitar tu ubicación. Revisa los permisos del navegador y vuelve a intentarlo.',
      )
      solicitudActiva.current = false
      setObteniendo(false)
    }
  }

  return {
    coordenadas,
    obteniendo,
    error,
    solicitarUbicacion,
  }
}
