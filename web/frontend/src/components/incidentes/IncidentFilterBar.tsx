import { useId, useMemo, useState } from 'react'
import {
  contarFiltrosActivos,
  crearFiltrosBarraVacios,
  obtenerCiudadesDisponibles,
  obtenerComunasDisponibles,
  resolverCambioCiudad,
  textoContadorFiltros,
  type FiltrosBarraIncidentes,
  type OpcionCiudadComunas,
  type OpcionFiltroIncidente,
} from './incidentFilterBar.utils'
import './IncidentFilterBar.css'

export type {
  FiltrosBarraIncidentes,
  OpcionCiudadComunas,
  OpcionFiltroIncidente,
}

export interface IncidentFilterBarProps {
  filtros: FiltrosBarraIncidentes
  ciudades: readonly OpcionCiudadComunas[]
  estados: readonly OpcionFiltroIncidente[]
  categorias: readonly OpcionFiltroIncidente[]
  onChange: (filtros: FiltrosBarraIncidentes) => void
  onLimpiar?: () => void
  defaultExpanded?: boolean
  disabled?: boolean
  titulo?: string
  ariaLabel?: string
  idPrefix?: string
  className?: string
}

export function IncidentFilterBar({
  filtros,
  ciudades,
  estados,
  categorias,
  onChange,
  onLimpiar,
  defaultExpanded = false,
  disabled = false,
  titulo = 'Filtros',
  ariaLabel = 'Filtros de incidentes',
  idPrefix,
  className,
}: IncidentFilterBarProps) {
  const autoId = useId()
  const baseId = idPrefix ?? `incident-filter-bar-${autoId}`
  const panelId = `${baseId}-panel`

  const [expandida, setExpandida] = useState(defaultExpanded)

  const cantidadActivos = contarFiltrosActivos(filtros)
  const hayActivos = cantidadActivos > 0
  const descripcionContador = textoContadorFiltros(cantidadActivos)

  const ciudadesDisponibles = useMemo(
    () => obtenerCiudadesDisponibles(ciudades),
    [ciudades],
  )

  const comunasDisponibles = useMemo(
    () => obtenerComunasDisponibles(ciudades, filtros.ciudad),
    [ciudades, filtros.ciudad],
  )

  const mostrarCiudadActual =
    Boolean(filtros.ciudad.trim()) &&
    !ciudadesDisponibles.includes(filtros.ciudad)

  const mostrarComunaActual =
    Boolean(filtros.comuna.trim()) &&
    !comunasDisponibles.includes(filtros.comuna)

  const mostrarEstadoActual =
    Boolean(filtros.estadoId.trim()) &&
    !estados.some((estado) => estado.id === filtros.estadoId)

  const mostrarCategoriaActual =
    Boolean(filtros.categoriaId.trim()) &&
    !categorias.some((categoria) => categoria.id === filtros.categoriaId)

  const handleToggle = () => {
    setExpandida((actual) => !actual)
  }

  const handleCiudadChange = (nuevaCiudad: string) => {
    onChange(resolverCambioCiudad(filtros, nuevaCiudad, ciudades))
  }

  const handleComunaChange = (nuevaComuna: string) => {
    onChange({
      ...filtros,
      comuna: nuevaComuna,
    })
  }

  const handleEstadoChange = (nuevoEstadoId: string) => {
    onChange({
      ...filtros,
      estadoId: nuevoEstadoId,
    })
  }

  const handleCategoriaChange = (nuevaCategoriaId: string) => {
    onChange({
      ...filtros,
      categoriaId: nuevaCategoriaId,
    })
  }

  const handleLimpiar = () => {
    onChange(crearFiltrosBarraVacios())
    onLimpiar?.()
  }

  const sinComunas =
    comunasDisponibles.length === 0 && !mostrarComunaActual

  const clasesContenedor = className
    ? `incident-filter-bar ${className}`
    : 'incident-filter-bar'

  return (
    <section className={clasesContenedor} aria-label={ariaLabel}>
      <div className="incident-filter-bar-header">
        <button
          type="button"
          className="incident-filter-bar-toggle"
          aria-expanded={expandida}
          aria-controls={panelId}
          onClick={handleToggle}
          data-testid="incident-filter-bar-toggle"
        >
          <span className="incident-filter-bar-title-group">
            <span className="incident-filter-bar-title">{titulo}</span>
            <span
              className={
                hayActivos
                  ? 'incident-filter-bar-badge incident-filter-bar-badge--active'
                  : 'incident-filter-bar-badge'
              }
              aria-label={descripcionContador}
              data-testid="incident-filter-bar-count"
            >
              {cantidadActivos}
            </span>
            <span
              className="incident-filter-bar-summary"
              aria-hidden="true"
            >
              {descripcionContador}
            </span>
          </span>

          <span
            className={
              expandida
                ? 'incident-filter-bar-chevron incident-filter-bar-chevron--open'
                : 'incident-filter-bar-chevron'
            }
            aria-hidden="true"
          >
            ▾
          </span>
        </button>

        <button
          type="button"
          className="incident-filter-bar-clear"
          onClick={handleLimpiar}
          disabled={disabled || !hayActivos}
          data-testid="incident-filter-bar-clear"
        >
          Limpiar filtros
        </button>
      </div>

      {expandida ? (
        <div
          id={panelId}
          className="incident-filter-bar-panel"
          role="group"
          aria-label="Controles de filtrado de incidentes"
          data-testid="incident-filter-bar-panel"
        >
          <div className="incident-filter-bar-field">
            <label htmlFor={`${baseId}-ciudad`}>Ciudad</label>
            <select
              id={`${baseId}-ciudad`}
              value={filtros.ciudad}
              disabled={disabled}
              onChange={(event) => handleCiudadChange(event.target.value)}
              data-testid="incident-filter-bar-ciudad"
            >
              <option value="">Todas las ciudades</option>
              {mostrarCiudadActual ? (
                <option value={filtros.ciudad}>{filtros.ciudad}</option>
              ) : null}
              {ciudadesDisponibles.map((ciudad) => (
                <option key={ciudad} value={ciudad}>
                  {ciudad}
                </option>
              ))}
            </select>
          </div>

          <div className="incident-filter-bar-field">
            <label htmlFor={`${baseId}-comuna`}>Comuna</label>
            <select
              id={`${baseId}-comuna`}
              value={filtros.comuna}
              disabled={disabled || sinComunas}
              onChange={(event) => handleComunaChange(event.target.value)}
              data-testid="incident-filter-bar-comuna"
            >
              <option value="">
                {sinComunas
                  ? 'Sin comunas disponibles'
                  : 'Todas las comunas'}
              </option>
              {mostrarComunaActual ? (
                <option value={filtros.comuna}>{filtros.comuna}</option>
              ) : null}
              {comunasDisponibles.map((comuna) => (
                <option key={comuna} value={comuna}>
                  {comuna}
                </option>
              ))}
            </select>
          </div>

          <div className="incident-filter-bar-field">
            <label htmlFor={`${baseId}-estado`}>Estado</label>
            <select
              id={`${baseId}-estado`}
              value={filtros.estadoId}
              disabled={disabled}
              onChange={(event) => handleEstadoChange(event.target.value)}
              data-testid="incident-filter-bar-estado"
            >
              <option value="">Todos los estados</option>
              {mostrarEstadoActual ? (
                <option value={filtros.estadoId}>Estado seleccionado</option>
              ) : null}
              {estados.map((estado) => (
                <option key={estado.id} value={estado.id}>
                  {estado.nombre}
                </option>
              ))}
            </select>
          </div>

          <div className="incident-filter-bar-field">
            <label htmlFor={`${baseId}-categoria`}>Categoría</label>
            <select
              id={`${baseId}-categoria`}
              value={filtros.categoriaId}
              disabled={disabled}
              onChange={(event) =>
                handleCategoriaChange(event.target.value)
              }
              data-testid="incident-filter-bar-categoria"
            >
              <option value="">Todas las categorías</option>
              {mostrarCategoriaActual ? (
                <option value={filtros.categoriaId}>
                  Categoría seleccionada
                </option>
              ) : null}
              {categorias.map((categoria) => (
                <option key={categoria.id} value={categoria.id}>
                  {categoria.nombre}
                </option>
              ))}
            </select>
          </div>
        </div>
      ) : null}
    </section>
  )
}
