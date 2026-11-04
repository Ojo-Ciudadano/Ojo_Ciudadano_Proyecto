import {
  ACCEPT_FOTOGRAFIAS,
  type EstadoFotografia,
  type FotografiaReporte,
} from '../../types/fotografiaReporte'
import './FotografiasReporte.css'

interface FotografiasReporteProps {
  fotografias: FotografiaReporte[]
  erroresSeleccion: string[]
  bloqueado: boolean
  incidenteCreado: boolean
  subiendo: boolean
  alSeleccionar: (archivos: FileList) => void
  alQuitar: (id: string) => void
  alReintentar: () => void
}

const TEXTOS_ESTADO: Record<EstadoFotografia, string> = {
  pendiente: 'Pendiente de subida',
  subiendo: 'Subiendo fotografía…',
  confirmada: 'Subida confirmada',
  fallida: 'Subida no confirmada',
}

function tamanoArchivo(bytes: number): string {
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`
  }

  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`
}

export function FotografiasReporte({
  fotografias,
  erroresSeleccion,
  bloqueado,
  incidenteCreado,
  subiendo,
  alSeleccionar,
  alQuitar,
  alReintentar,
}: FotografiasReporteProps) {
  const confirmadas = fotografias.filter(
    (fotografia) => fotografia.estado === 'confirmada',
  ).length

  const fallidas = fotografias.filter(
    (fotografia) => fotografia.estado === 'fallida',
  ).length

  return (
    <section
      className="reporte-fotos"
      aria-labelledby="reporte-fotos-titulo"
      aria-busy={subiendo}
    >
      <h2 id="reporte-fotos-titulo">
        Fotografías opcionales
      </h2>

      {!incidenteCreado && (
        <>
          <p id="reporte-fotos-ayuda">
            Puedes agregar fotografías JPEG, PNG o WebP de hasta
            10 MB cada una. También puedes crear el reporte sin
            fotografías.
          </p>

          <div className="nuevo-reporte-campo">
            <label htmlFor="reporte-fotos-archivos">
              Seleccionar fotografías
            </label>
            <input
              id="reporte-fotos-archivos"
              type="file"
              accept={ACCEPT_FOTOGRAFIAS}
              multiple
              disabled={bloqueado}
              aria-describedby="reporte-fotos-ayuda"
              onChange={(event) => {
                const archivos = event.currentTarget.files

                if (archivos?.length) {
                  alSeleccionar(archivos)
                }

                event.currentTarget.value = ''
              }}
            />
          </div>
        </>
      )}

      {erroresSeleccion.length > 0 && !incidenteCreado && (
        <div className="nuevo-reporte-error" role="alert">
          <p>No se agregaron estos archivos:</p>
          <ul>
            {erroresSeleccion.map((mensaje, indice) => (
              <li key={`${indice}-${mensaje}`}>
                {mensaje}
              </li>
            ))}
          </ul>
        </div>
      )}

      {fotografias.length > 0 ? (
        <>
          <p role="status" aria-live="polite">
            {incidenteCreado
              ? `${confirmadas} de ${fotografias.length} fotografías confirmadas.`
              : `${fotografias.length} fotografías seleccionadas.`}
            {subiendo && ' Subiendo fotografías…'}
          </p>

          <ul className="reporte-fotos-lista">
            {fotografias.map((fotografia) => (
              <li
                key={fotografia.id}
                className="reporte-fotos-tarjeta"
              >
                <img
                  src={fotografia.urlPrevisualizacion}
                  alt={`Previsualización de ${fotografia.archivo.name}`}
                  className="reporte-fotos-imagen"
                />

                <p className="reporte-fotos-nombre">
                  {fotografia.archivo.name}
                </p>

                <p>
                  {tamanoArchivo(fotografia.archivo.size)}
                </p>

                <p
                  className={`reporte-fotos-estado reporte-fotos-estado-${fotografia.estado}`}
                >
                  {TEXTOS_ESTADO[fotografia.estado]}
                </p>

                {fotografia.error && (
                  <p className="nuevo-reporte-error">
                    {fotografia.error}
                  </p>
                )}

                {!incidenteCreado && (
                  <button
                    type="button"
                    disabled={bloqueado}
                    onClick={() => alQuitar(fotografia.id)}
                    aria-label={`Quitar ${fotografia.archivo.name}`}
                  >
                    Quitar fotografía
                  </button>
                )}
              </li>
            ))}
          </ul>
        </>
      ) : (
        !incidenteCreado && (
          <p>No has seleccionado fotografías.</p>
        )
      )}

      {incidenteCreado && fallidas > 0 && !subiendo && (
        <div>
          <p className="nuevo-reporte-error" role="alert">
            El reporte ya está creado. No se confirmó la subida
            de {fallidas} fotografía(s). Puedes reintentar las
            fallidas; las confirmadas no se volverán a enviar.
          </p>

          <button
            type="button"
            onClick={alReintentar}
            disabled={bloqueado}
          >
            Reintentar fotografías fallidas
          </button>
        </div>
      )}
    </section>
  )
}