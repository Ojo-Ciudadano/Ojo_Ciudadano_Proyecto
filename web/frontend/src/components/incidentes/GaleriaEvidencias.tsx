import { useId } from 'react'
import {
  prepararFotografiasGaleria,
  type FotografiaGaleria,
} from '../../services/evidenciasIncidente.service'
import { FotografiaEvidencia } from './FotografiaEvidencia'
import './GaleriaEvidencias.css'

interface GaleriaEvidenciasProps {
  incidenteId: string
  evidencias: unknown
}

export function GaleriaEvidencias({
  incidenteId,
  evidencias,
}: GaleriaEvidenciasProps) {
  const tituloId = useId()

  let fotografias: FotografiaGaleria[] = []
  let errorColeccion = false

  try {
    fotografias = prepararFotografiasGaleria(
      evidencias,
      incidenteId,
    )
  } catch {
    errorColeccion = true
  }

  return (
    <section
      className="galeria-evidencias"
      aria-labelledby={tituloId}
    >
      <header className="galeria-evidencias-encabezado">
        <h2 id={tituloId}>Fotografías del incidente</h2>

        <p>Selecciona una fotografía para verla ampliada.</p>
      </header>

      {errorColeccion ? (
        <div
          className="galeria-evidencias-aviso galeria-evidencias-error"
          role="alert"
        >
          No se pudo interpretar la información de las fotografías.
          Los demás datos del incidente siguen disponibles.
        </div>
      ) : fotografias.length === 0 ? (
        <p
          className="galeria-evidencias-aviso"
          role="status"
        >
          Este incidente no tiene fotografías asociadas.
        </p>
      ) : (
        <ul
          className="galeria-evidencias-lista"
          aria-label="Fotografías asociadas al incidente"
        >
          {fotografias.map((fotografia) => (
            <li
              key={`${incidenteId}:${fotografia.numero}:${fotografia.evidencia?.id ?? 'invalida'}`}
              className="galeria-evidencias-elemento"
            >
              {fotografia.evidencia ? (
                <FotografiaEvidencia
                  evidencia={fotografia.evidencia}
                  numero={fotografia.numero}
                />
              ) : (
                <div
                  className="galeria-evidencias-aviso galeria-evidencias-error"
                  role="alert"
                >
                  {fotografia.error}
                </div>
              )}
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}