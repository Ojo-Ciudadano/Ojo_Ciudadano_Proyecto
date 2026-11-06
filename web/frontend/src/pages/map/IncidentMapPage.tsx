import { useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { IncidentMap } from '../../components/map/IncidentMap'
import {
  ORIGEN_NAVEGACION_MAPA,
  construirRutaDetalleIncidente,
} from '../../components/map/incidentPopup'
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
  const navigate = useNavigate()
  const {
    marcadores,
    cargando,
    error,
    reintentar,
    filtros,
    estados,
    categorias,
    hayFiltros,
    cambiarFiltro,
    restablecerFiltros,
    incidenteSeleccionadoId,
    seleccionarIncidente,
    totalIncidentes,
    totalGeorreferenciados,
    totalOmitidos,
  } = useIncidentesMapa()

  const handleOpenIncidentDetail = useCallback(
    (incidentId: string) => {
      const rutaDestino = construirRutaDetalleIncidente(incidentId)
      if (!rutaDestino) {
        return
      }
      navigate(rutaDestino, {
        state: { origen: ORIGEN_NAVEGACION_MAPA },
      })
    },
    [navigate],
  )

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

      <div
        className="mapa-filtros"
        role="group"
        aria-label="Filtros del mapa de incidentes"
      >
        <div className="mapa-filtro">
          <label htmlFor="mapa-filtro-estado">Estado</label>

          <select
            id="mapa-filtro-estado"
            value={filtros.estadoId}
            onChange={(event) =>
              cambiarFiltro('estadoId', event.target.value)
            }
          >
            <option value="">Todos los estados</option>

            {estados.map((estado) => (
              <option key={estado.id} value={estado.id}>
                {estado.nombre}
              </option>
            ))}
          </select>
        </div>

        <div className="mapa-filtro">
          <label htmlFor="mapa-filtro-categoria">Categoría</label>

          <select
            id="mapa-filtro-categoria"
            value={filtros.categoriaId}
            onChange={(event) =>
              cambiarFiltro('categoriaId', event.target.value)
            }
          >
            <option value="">Todas las categorías</option>

            {categorias.map((categoria) => (
              <option key={categoria.id} value={categoria.id}>
                {categoria.nombre}
              </option>
            ))}
          </select>
        </div>

        <button
          type="button"
          className="mapa-restablecer"
          onClick={restablecerFiltros}
          disabled={!hayFiltros}
        >
          Restablecer filtros
        </button>
      </div>

      <div className="mapa-tarjeta" aria-busy={cargando}>
        <IncidentMap
          markers={marcadores}
          selectedMarkerId={incidenteSeleccionadoId}
          onSelectMarker={seleccionarIncidente}
          onOpenIncidentDetail={handleOpenIncidentDetail}
        />

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
              <p>
                {hayFiltros
                  ? 'No se encontraron incidentes georreferenciados con los filtros seleccionados.'
                  : 'No hay incidentes georreferenciados para mostrar.'}
              </p>
              {totalIncidentes > 0 && (
                <p className="mapa-overlay__detalle">
                  {hayFiltros
                    ? `Se encontraron ${totalIncidentes} incidentes para los filtros aplicados, pero ninguno informa coordenadas válidas.`
                    : `Se encontraron ${totalIncidentes} incidentes, pero ninguno informa coordenadas válidas.`}
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