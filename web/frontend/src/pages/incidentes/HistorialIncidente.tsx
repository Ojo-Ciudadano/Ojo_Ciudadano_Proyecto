import { useEffect, useState } from 'react'
import {
  normalizarHistorialIncidente,
  obtenerHistorialIncidente,
} from '../../services/incidentes.service'
import type { RegistroHistorialIncidente } from '../../types/incidenteDetalle'

interface HistorialIncidenteProps {
  incidenteId: string
  historialIncluido?: RegistroHistorialIncidente[] | null
}

interface ErrorHistorial {
  mensaje: string
  permiteReintentar: boolean
}

interface ResultadoHistorialIndependiente {
  clave: string
  registros: RegistroHistorialIncidente[] | null
  error: ErrorHistorial | null
}

type TipoMovimientoHistorial =
  | 'asignacion'
  | 'reasignacion'
  | 'cambio-estado'

const PATRON_ASIGNACION = /^Asignado a\s+\S+/i
const PATRON_REASIGNACION = /^Reasignado de\s+\S+\s+a\s+\S+/i

function clasificarMovimiento(
  registro: RegistroHistorialIncidente,
): TipoMovimientoHistorial {
  const tipoExplicito = (
    registro.tipo ??
    registro.tipoEvento ??
    ''
  )
    .trim()
    .toUpperCase()

  if (tipoExplicito === 'REASIGNACION') {
    return 'reasignacion'
  }

  if (tipoExplicito === 'ASIGNACION') {
    return 'asignacion'
  }

  const observacion = registro.observacion?.trim() ?? ''

  if (PATRON_REASIGNACION.test(observacion)) {
    return 'reasignacion'
  }

  if (PATRON_ASIGNACION.test(observacion)) {
    return 'asignacion'
  }

  return 'cambio-estado'
}

function etiquetaTipoMovimiento(
  tipo: TipoMovimientoHistorial,
): string {
  if (tipo === 'asignacion') {
    return 'Asignación de responsable'
  }

  if (tipo === 'reasignacion') {
    return 'Reasignación de responsable'
  }

  return 'Cambio de estado'
}

function formatearFechaHistorial(valor?: string | null): string {
  if (!valor) return 'Sin información'

  const fechaInterpretada = new Date(valor)

  if (Number.isNaN(fechaInterpretada.getTime())) {
    return 'Sin información'
  }

  return new Intl.DateTimeFormat('es-CL', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(fechaInterpretada)
}

function textoActor(usuarioId?: string | null): string {
  return usuarioId?.trim() || 'Sin información'
}

function deduplicarManteniendoOrden(
  registros: RegistroHistorialIncidente[],
): RegistroHistorialIncidente[] {
  const vistos = new Set<string>()
  const resultado: RegistroHistorialIncidente[] = []

  for (const registro of registros) {
    if (vistos.has(registro.id)) {
      continue
    }
    vistos.add(registro.id)
    resultado.push(registro)
  }

  return resultado
}

function interpretarErrorHistorial(error: unknown): ErrorHistorial {
  const statusCode =
    typeof error === 'object' &&
    error !== null &&
    'statusCode' in error
      ? Number(error.statusCode)
      : undefined

  if (statusCode === 400) {
    return {
      mensaje: 'El identificador del incidente no es válido para consultar el historial.',
      permiteReintentar: false,
    }
  }

  if (statusCode === 401) {
    return {
      mensaje: 'Tu sesión no es válida. Ingresa nuevamente.',
      permiteReintentar: false,
    }
  }

  if (statusCode === 403) {
    return {
      mensaje: 'No tienes permisos para consultar el historial de este incidente.',
      permiteReintentar: false,
    }
  }

  return {
    mensaje: 'No se pudo cargar el historial del incidente. Inténtalo nuevamente.',
    permiteReintentar: true,
  }
}

export function HistorialIncidente({
  incidenteId,
  historialIncluido,
}: HistorialIncidenteProps) {
  const [intento, setIntento] = useState(0)
  const [consultaIndependiente, setConsultaIndependiente] =
    useState<ResultadoHistorialIndependiente | null>(null)

  const historialDelDetalleValido =
    historialIncluido !== undefined && historialIncluido !== null
      ? normalizarHistorialIncidente(historialIncluido, incidenteId)
      : null

  const requiereConsultaIndependiente =
    historialDelDetalleValido === null

  const claveConsulta = JSON.stringify([incidenteId, intento])

  useEffect(() => {
    if (!requiereConsultaIndependiente) {
      return
    }

    const controller = new AbortController()

    async function cargarHistorial() {
      try {
        const registros = await obtenerHistorialIncidente(
          incidenteId,
          controller.signal,
        )

        if (controller.signal.aborted) return

        setConsultaIndependiente({
          clave: claveConsulta,
          registros,
          error: null,
        })
      } catch (error: unknown) {
        if (controller.signal.aborted) return

        setConsultaIndependiente({
          clave: claveConsulta,
          registros: null,
          error: interpretarErrorHistorial(error),
        })
      }
    }

    void cargarHistorial()

    return () => controller.abort()
  }, [incidenteId, claveConsulta, requiereConsultaIndependiente])

  const resultadoIndependienteActual =
    consultaIndependiente?.clave === claveConsulta
      ? consultaIndependiente
      : null

  const cargando =
    requiereConsultaIndependiente &&
    resultadoIndependienteActual === null

  const error = requiereConsultaIndependiente
    ? resultadoIndependienteActual?.error ?? null
    : null

  const registrosOrigen = requiereConsultaIndependiente
    ? resultadoIndependienteActual?.registros ?? null
    : historialDelDetalleValido

  const registros = registrosOrigen
    ? deduplicarManteniendoOrden(registrosOrigen)
    : null

  function reintentar() {
    setIntento((actual) => actual + 1)
  }

  return (
    <section
      className="detalle-tarjeta detalle-ancho-completo detalle-historial"
      aria-labelledby="detalle-historial-titulo"
      aria-busy={cargando}
      data-incidente-historial-id={incidenteId}
    >
      <h2 id="detalle-historial-titulo">Historial del incidente</h2>

      {cargando ? (
        <div
          className="historial-mensaje"
          role="status"
        >
          Cargando historial del incidente…
        </div>
      ) : error ? (
        <div
          className="historial-mensaje historial-error"
          role="alert"
        >
          <p>{error.mensaje}</p>

          {error.permiteReintentar && (
            <button type="button" onClick={reintentar}>
              Reintentar historial
            </button>
          )}
        </div>
      ) : !registros || registros.length === 0 ? (
        <p className="historial-vacio">
          Este incidente aún no registra movimientos en su historial.
        </p>
      ) : (
        <ol
          className="historial-cronologia"
          aria-label="Línea de tiempo del historial del incidente"
        >
          {registros.map((registro) => {
            const tipoMovimiento = clasificarMovimiento(registro)
            const esEventoAsignacion =
              tipoMovimiento === 'asignacion' ||
              tipoMovimiento === 'reasignacion'
            const nombreEstado =
              registro.estado?.nombre?.trim() || 'Sin información'
            const observacionLimpia = registro.observacion?.trim()

            return (
              <li
                key={registro.id}
                className={`historial-entrada ${
                  esEventoAsignacion
                    ? 'historial-entrada-asignacion'
                    : 'historial-entrada-estado'
                }`}
                data-tipo-evento={tipoMovimiento}
              >
                <div
                  className="historial-marcador"
                  aria-hidden="true"
                />

                <div className="historial-contenido">
                  <div className="historial-encabezado">
                    <span
                      className={`historial-etiqueta-tipo ${
                        esEventoAsignacion
                          ? 'historial-etiqueta-asignacion'
                          : 'historial-etiqueta-estado'
                      }`}
                    >
                      {etiquetaTipoMovimiento(tipoMovimiento)}
                    </span>

                    <span className="historial-fecha">
                      {formatearFechaHistorial(registro.fecha)}
                    </span>
                  </div>

                  <dl className="detalle-datos historial-datos">
                    <div>
                      <dt>
                        {esEventoAsignacion
                          ? 'Estado al momento de la asignación'
                          : 'Estado'}
                      </dt>
                      <dd>{nombreEstado}</dd>
                    </div>

                    <div>
                      <dt>Usuario actor</dt>
                      <dd>{textoActor(registro.usuarioId)}</dd>
                    </div>

                    <div className="historial-observacion-bloque">
                      <dt>Observación</dt>
                      <dd>
                        {observacionLimpia ? (
                          observacionLimpia
                        ) : (
                          <span className="historial-sin-observacion">
                            Sin observación registrada.
                          </span>
                        )}
                      </dd>
                    </div>
                  </dl>
                </div>
              </li>
            )
          })}
        </ol>
      )}
    </section>
  )
}