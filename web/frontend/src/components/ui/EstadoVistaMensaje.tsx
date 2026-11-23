import type { ReactNode } from 'react'

export type TipoEstadoVista = 'loading' | 'empty' | 'error'

export interface EstadoVistaMensajeProps {
  tipo: TipoEstadoVista
  titulo?: string
  mensaje: string
  children?: ReactNode
  className?: string
  headingLevel?: 'h1' | 'h2' | 'h3'
  dataAccesoDenegado?: 'true'
}

export function EstadoVistaMensaje({
  tipo,
  titulo,
  mensaje,
  children,
  className,
  headingLevel = 'h2',
  dataAccesoDenegado,
}: EstadoVistaMensajeProps) {
  const role = tipo === 'error' ? 'alert' : 'status'
  const clases = className
    ? `ui-state-box ui-state-box--${tipo} ${className}`
    : `ui-state-box ui-state-box--${tipo}`

  const renderTitulo = () => {
    if (!titulo) return null
    if (headingLevel === 'h1') {
      return <h1 className="ui-state-box-title">{titulo}</h1>
    }
    if (headingLevel === 'h3') {
      return <h3 className="ui-state-box-title">{titulo}</h3>
    }
    return <h2 className="ui-state-box-title">{titulo}</h2>
  }

  return (
    <div
      className={clases}
      role={role}
      data-estado-vista={tipo}
      data-acceso-denegado={dataAccesoDenegado}
    >
      {tipo === 'loading' ? (
        <span className="ui-spinner" aria-hidden="true" />
      ) : null}
      {renderTitulo()}
      <p className="ui-state-box-message">{mensaje}</p>
      {children ? (
        <div className="ui-state-box-actions">{children}</div>
      ) : null}
    </div>
  )
}
