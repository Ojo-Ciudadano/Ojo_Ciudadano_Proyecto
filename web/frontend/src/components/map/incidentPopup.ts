import type { IncidentMapMarker } from './mapConfig'

/**
 * Utilidades del popup resumen de marcadores (#141) y navegación al detalle (#142).
 *
 * El contenido se construye con nodos de texto (`textContent`), nunca con
 * `innerHTML`, para que títulos u otros datos de la API no puedan inyectar
 * HTML. El módulo no importa Leaflet en tiempo de ejecución, de modo que sus
 * funciones puras pueden probarse con `node:test`.
 */

export const TEXTO_CATEGORIA_DESCONOCIDA = 'Sin categoría'
export const TEXTO_ESTADO_DESCONOCIDO = 'Sin estado'
export const TEXTO_FECHA_DESCONOCIDA = 'Fecha no informada'
export const TEXTO_ACCION_DETALLE = 'Ver detalle'
export const ORIGEN_NAVEGACION_MAPA = 'mapa' as const

export interface EstadoNavegacionDetalle {
  origen?: string
}

export interface ResumenIncidentePopup {
  incidentId: string
  titulo: string
  categoria: string
  estado: string
  fechaCreacion: string
}

export interface CampoResumenPopup {
  etiqueta: string
  valor: string
}

export interface EstadoSeleccionPopup {
  incidenteSeleccionadoId: string | null
}

export const ESTADO_SELECCION_INICIAL: EstadoSeleccionPopup = {
  incidenteSeleccionadoId: null,
}

export type AccionSeleccionPopup =
  | { tipo: 'seleccionar'; incidenteId: string }
  | { tipo: 'cerrar'; incidenteId: string }
  | { tipo: 'limpiar' }

/**
 * Máquina de estados de la selección del mapa.
 *
 * `seleccionar` reemplaza la selección anterior (un solo popup a la vez).
 * `cerrar` solo limpia si el incidente cerrado es el seleccionado.
 * `limpiar` descarta la selección al refrescar los marcadores.
 */
export function reducirSeleccionPopup(
  estado: EstadoSeleccionPopup,
  accion: AccionSeleccionPopup,
): EstadoSeleccionPopup {
  switch (accion.tipo) {
    case 'seleccionar':
      return { incidenteSeleccionadoId: accion.incidenteId }
    case 'cerrar':
      return estado.incidenteSeleccionadoId === accion.incidenteId
        ? ESTADO_SELECCION_INICIAL
        : estado
    case 'limpiar':
      return ESTADO_SELECCION_INICIAL
  }
}

function textoOpcional(valor: unknown): string | undefined {
  if (typeof valor !== 'string') {
    return undefined
  }
  const texto = valor.trim()
  return texto === '' ? undefined : texto
}

const formateadorFecha = new Intl.DateTimeFormat('es-CL', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  timeZone: 'America/Santiago',
})

/** Formatea la fecha de creación en un texto legible o un respaldo seguro. */
export function formatearFechaCreacion(valor?: string | null): string {
  const texto = textoOpcional(valor)
  if (!texto) {
    return TEXTO_FECHA_DESCONOCIDA
  }
  const fecha = new Date(texto)
  if (Number.isNaN(fecha.getTime())) {
    return TEXTO_FECHA_DESCONOCIDA
  }
  return formateadorFecha.format(fecha)
}

/** Resume los datos de un marcador para el popup, con respaldos legibles. */
export function construirResumenIncidente(
  marcador: IncidentMapMarker,
): ResumenIncidentePopup {
  return {
    incidentId: marcador.id,
    titulo: textoOpcional(marcador.titulo) ?? `Incidente ${marcador.id}`,
    categoria: textoOpcional(marcador.categoria) ?? TEXTO_CATEGORIA_DESCONOCIDA,
    estado: textoOpcional(marcador.estado) ?? TEXTO_ESTADO_DESCONOCIDO,
    fechaCreacion: formatearFechaCreacion(marcador.fechaCreacion),
  }
}

/** Campos etiquetados que se muestran bajo el título del popup. */
export function construirCamposResumen(
  resumen: ResumenIncidentePopup,
): CampoResumenPopup[] {
  return [
    { etiqueta: 'Categoría', valor: resumen.categoria },
    { etiqueta: 'Estado', valor: resumen.estado },
    { etiqueta: 'Fecha de creación', valor: resumen.fechaCreacion },
  ]
}

/**
 * Construye la ruta de detalle `/incidentes/:id` validando que el identificador
 * sea una cadena no vacía (#142). Devuelve `null` si el id es inválido.
 */
export function construirRutaDetalleIncidente(
  incidentId: unknown,
): string | null {
  const idLimpio = textoOpcional(incidentId)
  if (!idLimpio) {
    return null
  }
  return `/incidentes/${encodeURIComponent(idLimpio)}`
}

/**
 * Resuelve qué incidente debe abrirse al accionar el botón del popup (#142).
 *
 * Si hay un marcador activo en `estadoSeleccion` y no coincide con el
 * `incidenteSolicitadoId` (por ejemplo, si el usuario cambió rápidamente de
 * marcador antes de que se cerrara el popup anterior), se prioriza el marcador
 * actualmente seleccionado o se rechaza un identificador vacío.
 */
export function resolverIncidenteNavegacion(
  estadoSeleccion: EstadoSeleccionPopup,
  incidenteSolicitadoId: unknown,
): string | null {
  const solicitado = textoOpcional(incidenteSolicitadoId)
  const seleccionado = textoOpcional(estadoSeleccion.incidenteSeleccionadoId)

  if (seleccionado) {
    return seleccionado
  }

  return solicitado ?? null
}

/** Subconjunto de `Document` necesario para construir el contenido. */
export interface DocumentoPopup {
  createElement(tagName: string): HTMLElement
}

export interface OpcionesContenidoPopup {
  onVerDetalle?: (incidentId: string) => void
}

/**
 * Construye un nodo seguro para el tooltip del marcador usando `textContent`
 * para evitar que Leaflet interprete cadenas como HTML (`innerHTML`).
 */
export function crearContenidoTooltip(
  marcador: IncidentMapMarker,
  documento: DocumentoPopup,
): HTMLElement {
  const resumen = construirResumenIncidente(marcador)
  const span = documento.createElement('span')
  span.className = 'incident-map__tooltip'
  span.textContent = resumen.titulo
  return span
}

/**
 * Construye el contenido del popup con `textContent` en todos los campos.
 * Conserva el identificador del incidente en `data-incident-id` e incorpora
 * el botón de acción para abrir el detalle del incidente (#141, #142).
 */
export function crearContenidoPopup(
  marcador: IncidentMapMarker,
  documento: DocumentoPopup,
  opciones?: OpcionesContenidoPopup,
): HTMLElement {
  const resumen = construirResumenIncidente(marcador)

  const contenedor = documento.createElement('div')
  contenedor.className = 'incident-map__popup'
  contenedor.setAttribute('data-incident-id', resumen.incidentId)

  const titulo = documento.createElement('strong')
  titulo.className = 'incident-map__popup-titulo'
  titulo.textContent = resumen.titulo
  contenedor.appendChild(titulo)

  const datos = documento.createElement('dl')
  datos.className = 'incident-map__popup-datos'

  for (const campo of construirCamposResumen(resumen)) {
    const termino = documento.createElement('dt')
    termino.textContent = campo.etiqueta

    const detalle = documento.createElement('dd')
    detalle.textContent = campo.valor

    datos.appendChild(termino)
    datos.appendChild(detalle)
  }

  contenedor.appendChild(datos)

  const botonDetalle = documento.createElement('button') as HTMLButtonElement
  botonDetalle.className = 'incident-map__popup-accion'
  botonDetalle.setAttribute('type', 'button')
  botonDetalle.setAttribute('data-incident-id', resumen.incidentId)
  botonDetalle.setAttribute(
    'aria-label',
    `${TEXTO_ACCION_DETALLE}: ${resumen.titulo}`,
  )
  botonDetalle.textContent = TEXTO_ACCION_DETALLE

  if (typeof botonDetalle.addEventListener === 'function') {
    botonDetalle.addEventListener('click', () => {
      opciones?.onVerDetalle?.(resumen.incidentId)
    })
  }

  contenedor.appendChild(botonDetalle)

  return contenedor
}
