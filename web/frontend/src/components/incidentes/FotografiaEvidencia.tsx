import { useEffect, useId, useRef, useState } from 'react'
import { useFotografiaEvidencia } from '../../hooks/useFotografiaEvidencia'
import type { EvidenciaIncidente } from '../../types/evidenciaIncidente'
import './FotografiaEvidencia.css'

interface FotografiaEvidenciaProps {
  evidencia: EvidenciaIncidente
  numero: number
}

interface VistaAmpliadaProps {
  url: string
  descripcion: string
  onCerrar: () => void
  onError: () => void
}

function VistaAmpliada({
  url,
  descripcion,
  onCerrar,
  onError,
}: VistaAmpliadaProps) {
  const dialogRef = useRef<HTMLDialogElement>(null)
  const tituloId = useId()

  useEffect(() => {
    const dialog = dialogRef.current

    if (!dialog) return

    const elementoAnterior = document.activeElement

    dialog.showModal()

    return () => {
      dialog.close()

      if (
        elementoAnterior instanceof HTMLElement &&
        elementoAnterior.isConnected
      ) {
        elementoAnterior.focus()
      }
    }
  }, [])

  return (
    <dialog
      ref={dialogRef}
      className="evidencia-modal"
      aria-labelledby={tituloId}
      onCancel={(event) => {
        event.preventDefault()
        onCerrar()
      }}
      onClick={(event) => {
        if (event.target === event.currentTarget) {
          onCerrar()
        }
      }}
    >
      <div className="evidencia-modal-contenido">
        <header className="evidencia-modal-encabezado">
          <h2 id={tituloId}>{descripcion}</h2>

          <button
            type="button"
            className="evidencia-boton"
            onClick={onCerrar}
            autoFocus
          >
            Cerrar
          </button>
        </header>

        <img
          src={url}
          alt={descripcion}
          className="evidencia-imagen-ampliada"
          onError={onError}
        />
      </div>
    </dialog>
  )
}

export function FotografiaEvidencia({
  evidencia,
  numero,
}: FotografiaEvidenciaProps) {
  const {
    url,
    error,
    cargando,
    sesionDisponible,
    reintentar,
    informarErrorImagen,
  } = useFotografiaEvidencia(evidencia)

  const [urlAmpliada, setUrlAmpliada] = useState<string | null>(null)

  const descripcion = `Fotografía ${numero} del incidente`

  function cerrarVistaAmpliada() {
    setUrlAmpliada(null)
  }

  function manejarErrorImagen() {
    cerrarVistaAmpliada()
    informarErrorImagen()
  }

  return (
    <figure className="evidencia-fotografia">
      <div
        className="evidencia-fotografia-contenido"
        aria-busy={cargando}
      >
        {cargando ? (
          <p className="evidencia-aviso" role="status">
            Cargando fotografía {numero}…
          </p>
        ) : !sesionDisponible ? (
          <p className="evidencia-aviso" role="status">
            Inicia sesión para consultar esta fotografía.
          </p>
        ) : error ? (
          <div className="evidencia-aviso evidencia-error" role="alert">
            <p>{error.mensaje}</p>

            {error.permiteReintentar && (
              <button
                type="button"
                className="evidencia-boton"
                aria-label={`Reintentar fotografía ${numero}`}
                onClick={reintentar}
              >
                Reintentar
              </button>
            )}
          </div>
        ) : url ? (
          <button
            type="button"
            className="evidencia-miniatura"
            aria-label={`Ampliar fotografía ${numero} del incidente`}
            onClick={() => setUrlAmpliada(url)}
          >
            <img
              src={url}
              alt={descripcion}
              onError={manejarErrorImagen}
            />

            <span className="evidencia-ampliar">
              Ampliar fotografía
            </span>
          </button>
        ) : null}
      </div>

      <figcaption className="evidencia-pie">
        Fotografía {numero}
      </figcaption>

      {url && urlAmpliada === url && (
        <VistaAmpliada
          key={url}
          url={url}
          descripcion={descripcion}
          onCerrar={cerrarVistaAmpliada}
          onError={manejarErrorImagen}
        />
      )}
    </figure>
  )
}