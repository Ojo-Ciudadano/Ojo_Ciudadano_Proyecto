import { IncidentMap } from '../../components/map/IncidentMap'
import type { IncidentMapMarker } from '../../components/map/mapConfig'
import './IncidentMapPage.css'

/**
 * Capa de marcadores inicial. La Issue #139 entrega la pantalla base; cuando
 * exista la consulta de incidentes con coordenadas, basta con reemplazar este
 * arreglo por los datos reales sin tocar la inicialización del mapa.
 */
const MARCADORES_INICIALES: readonly IncidentMapMarker[] = []

const LEYENDA = [
  { color: '#dc2626', etiqueta: 'Prioridad alta' },
  { color: '#f97316', etiqueta: 'Prioridad media' },
  { color: '#1d4ed8', etiqueta: 'Prioridad baja' },
]

export function IncidentMapPage() {
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
        <IncidentMap markers={MARCADORES_INICIALES} />
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
          Cartografía base: OpenStreetMap. La capa de marcadores se activará
          cuando los incidentes informen coordenadas geográficas.
        </p>
      </div>
    </section>
  )
}
