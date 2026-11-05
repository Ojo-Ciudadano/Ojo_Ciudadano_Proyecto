import { useId } from 'react'
import { normalizarEvidenciasIncidente } from '../../services/evidenciasIncidente.service'
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

  const resultado = (() => {
    try {
      return {
        evidencias: normalizarEvidenciasIncidente(
          evidencias,
          incidenteId,
        ),
        error: false,
      }
    } catch {
      return {
        evidencias: [],
        error: true,
      }
    }
  })()

  return (
    <section
      className="galeria-evidencias"
      aria-labelledby={tituloId}
    >
      <header className="galeria-evidencias-encabezado">
        <h2 id={tituloId}>Fotografías del incidente</h2>

        <p>
          Selecciona una fotografía para verla ampliada.
        </p>
      </header>

      {resultado.error ? (
        <div
          className="galeria-evidencias-aviso galeria-evidencias-error"
          role="alert"
        >
          No se pudo interpretar la información de las fotografías.
          Puedes volver a consultar el detalle del incidente.
        </div>
      ) : resultado.evidencias.length === 0 ? (
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
          {resultado.evidencias.map((evidencia, indice) => (
            <li
              key={`${incidenteId}:${evidencia.id}`}
              className="galeria-evidencias-elemento"
            >
              <FotografiaEvidencia
                evidencia={evidencia}
                numero={indice + 1}
              />
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}