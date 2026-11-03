import { IncidentMap } from '../../components/map/IncidentMap'
import { useIncidentesMapa } from '../../hooks/useIncidentesMapa'
import './IncidentMapPage.css'

const LEYENDA = [
  { color: '#dc2626', etiqueta: 'Prioridad alta' },
  { color: '#f97316', etiqueta: 'Prioridad media' },
  { color: '#1d4ed8', etiqueta: 'Prioridad baja' },
]

function resumenIncidentes(
  totalGeorreferenciados: number,
  totalIncidentes: number,
  totalOmitidos: number,
): string {
  if (totalGeorreferenciados === 0) {
    return 'Cartografía base: OpenStreetMap.'
  }

  const base = `Mostrando ${totalGeorreferenciados} de ${totalIncidentes} incidentes con coordenadas válidas.`

  if (totalOmitidos === 0) {
    return `${base} Cartografía base: OpenStreetMap.`
  }

  return `${base} ${totalOmitidos} sin ubicación geográfica válida. Cartografía base: OpenStreetMap.`
}

export function IncidentMapPage() {
  const {
    marcadores,
    cargando,
    error,
    reintentar,
    totalIncidentes,
    totalGeorreferenciados,
    totalOmitidos,
  } = useIncidentesMapa()

  const mostrarVacio =
    !cargando && !error && totalGeorreferenciados === 0

  return (
    <section className="mapa-pagina">
      <header className="mapa-encabezado">
        <h1>Mapa de incidentes</h1>
        <p>
          Explora la Región de La Araucanía y ubica los reportes ciudadanos por
          comuna. Arrastra el mapa para desplazarte y usa los controles o la
          rueda del mouse para acercar y alejar.
        </p>
      </header>

      <div className="mapa-tarjeta">
        <IncidentMap markers={marcadores} />

        {cargando && (
          <div className="mapa-overlay" role="status" aria-live="polite">
            <div className="mapa-overlay__panel">
              <span className="mapa-spinner" aria-hidden="true" />
              <p>Cargando incidentes georreferenciados…</p>
            </div>
          </div>
        )}

        {!cargando && error && (
          <div className="mapa-overlay" role="alert">
            <div className="mapa-overlay__panel">
              <p>{error.mensaje}</p>
              {error.permiteReintentar && (
                <button
                  type="button"
                  className="mapa-overlay__retry"
                  onClick={reintentar}
                >
                  Reintentar
                </button>
              )}
            </div>
          </div>
        )}

        {mostrarVacio && (
          <div
            className="mapa-overlay mapa-overlay--vacio"
            role="status"
            aria-live="polite"
          >
            <div className="mapa-overlay__panel">
              <p>No hay incidentes georreferenciados para mostrar.</p>
              {totalIncidentes > 0 && (
                <p className="mapa-overlay__detalle">
                  Se encontraron {totalIncidentes} incidentes, pero ninguno
                  informa coordenadas válidas.
                </p>
              )}
            </div>
          </div>
        )}
      </div>

      <div className="mapa-pie">
        <ul className="mapa-leyenda" aria-label="Leyenda de prioridades">
          {LEYENDA.map((item) => (
            <li key={item.etiqueta}>
              <span
                className="mapa-leyenda__punto"
                style={{ backgroundColor: item.color }}
                aria-hidden="true"
              />
              {item.etiqueta}
            </li>
          ))}
        </ul>

        <p className="mapa-nota">
          {resumenIncidentes(
            totalGeorreferenciados,
            totalIncidentes,
            totalOmitidos,
          )}
        </p>
      </div>
    </section>
  )
}
