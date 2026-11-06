import { useEffect, useState, useSyncExternalStore } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import { useFotografiaEvidencia } from '../../hooks/useFotografiaEvidencia'
import {
  getSessionEpoch,
  subscribeSessionChanges,
} from '../../services/api'
import {
  interpretarErrorEvidencia,
  normalizarEvidenciasIncidente,
} from '../../services/evidenciasIncidente.service'
import { obtenerIncidenteDetalle } from '../../services/incidentes.service'
import type { EvidenciaIncidente } from '../../types/evidenciaIncidente'
import './CarruselEvidencias.css'

interface Props {
  incidenteId: string
}

function ImagenCarrusel({
  evidencia,
  numero,
}: {
  evidencia: EvidenciaIncidente
  numero: number
}) {
  const {
    url,
    error,
    cargando,
    sesionDisponible,
    reintentar,
    informarErrorImagen,
  } = useFotografiaEvidencia(evidencia)

  return (
    <div className="carrusel-evidencias-imagen">
      {cargando ? (
        <p role="status">Cargando fotografía…</p>
      ) : !sesionDisponible ? (
        <p role="status">Inicia sesión para ver la fotografía.</p>
      ) : error ? (
        <div role="alert">
          <p>{error.mensaje}</p>

          {error.permiteReintentar && (
            <button type="button" onClick={reintentar}>
              Reintentar fotografía
            </button>
          )}
        </div>
      ) : url ? (
        <Link
          to={`/incidentes/${encodeURIComponent(evidencia.incidenteId)}`}
          aria-label={`Ver detalle del incidente con la fotografía ${numero}`}
        >
          <img
            src={url}
            alt={`Fotografía ${numero} del incidente`}
            onError={informarErrorImagen}
          />
        </Link>
      ) : null}
    </div>
  )
}

function ContenidoCarrusel({
  incidenteId,
  reintentar,
}: Props & { reintentar: () => void }) {
  const [resultado, setResultado] = useState<{
    evidencias: EvidenciaIncidente[]
    error: ReturnType<typeof interpretarErrorEvidencia> | null
  } | null>(null)

  const [indice, setIndice] = useState(0)

  useEffect(() => {
    const controller = new AbortController()

    async function cargar() {
      try {
        const detalle = await obtenerIncidenteDetalle(
          incidenteId,
          controller.signal,
        )

        const evidencias = normalizarEvidenciasIncidente(
          detalle.evidencias ?? [],
          incidenteId,
        )

        if (!controller.signal.aborted) {
          setResultado({ evidencias, error: null })
        }
      } catch (error: unknown) {
        if (!controller.signal.aborted) {
          setResultado({
            evidencias: [],
            error: interpretarErrorEvidencia(error),
          })
        }
      }
    }

    void cargar()

    return () => controller.abort()
  }, [incidenteId])

  if (!resultado) {
    return (
      <div className="carrusel-evidencias-aviso" role="status">
        Consultando fotografías…
      </div>
    )
  }

  if (resultado.error) {
    return (
      <div className="carrusel-evidencias-aviso" role="alert">
        <p>No se pudieron consultar las fotografías.</p>

        {resultado.error.permiteReintentar && (
          <button type="button" onClick={reintentar}>
            Reintentar
          </button>
        )}
      </div>
    )
  }

  const { evidencias } = resultado
  const total = evidencias.length

  if (total === 0) {
    return (
      <p className="carrusel-evidencias-aviso">
        Sin fotografías asociadas.
      </p>
    )
  }

  const evidencia = evidencias[indice]

  return (
    <section
      className="carrusel-evidencias"
      aria-label="Fotografías del reporte"
      aria-roledescription="carrusel"
    >
      <div className="carrusel-evidencias-marco">
        <ImagenCarrusel
          key={evidencia.id}
          evidencia={evidencia}
          numero={indice + 1}
        />

        <span className="carrusel-evidencias-contador">
          {indice + 1}/{total}
        </span>
      </div>

      {total > 1 && (
        <div className="carrusel-evidencias-controles">
          <button
            type="button"
            aria-label="Fotografía anterior"
            onClick={() =>
              setIndice((actual) => (actual - 1 + total) % total)
            }
          >
            ← Anterior
          </button>

          <span role="status" aria-live="polite" aria-atomic="true">
            Fotografía {indice + 1} de {total}
          </span>

          <button
            type="button"
            aria-label="Fotografía siguiente"
            onClick={() =>
              setIndice((actual) => (actual + 1) % total)
            }
          >
            Siguiente →
          </button>
        </div>
      )}
    </section>
  )
}

export function CarruselEvidencias({ incidenteId }: Props) {
  const { user, isAuthenticated, isLoading } = useAuth()
  const [intento, setIntento] = useState(0)

  const epocaSesion = useSyncExternalStore(
    subscribeSessionChanges,
    getSessionEpoch,
    () => 0,
  )

  if (isLoading || !isAuthenticated || !user?.id) {
    return null
  }

  const clave = JSON.stringify([
    incidenteId,
    user.id,
    epocaSesion,
    intento,
  ])

  return (
    <ContenidoCarrusel
      key={clave}
      incidenteId={incidenteId}
      reintentar={() => setIntento((actual) => actual + 1)}
    />
  )
}