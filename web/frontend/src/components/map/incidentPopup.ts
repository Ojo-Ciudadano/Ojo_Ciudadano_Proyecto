import type { IncidentMapMarker } from './mapConfig'
import { textoOpcional } from './textoOpcional'

/**
 * Utilidades del popup resumen de marcadores (#141).
 *
 * El contenido se construye con nodos de texto (`textContent`), nunca con
 * `innerHTML`, para que títulos u otros datos de la API no puedan inyectar
 * HTML. El módulo no importa Leaflet en tiempo de ejecución, de modo que sus
 * funciones puras pueden probarse con `node:test`.
 */

export const TEXTO_CATEGORIA_DESCONOCIDA = 'Sin categoría'
export const TEXTO_ESTADO_DESCONOCIDO = 'Sin estado'
export const TEXTO_FECHA_DESCONOCIDA = 'Fecha no informada'

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
 * - `seleccionar` reemplaza la selección anterior (un solo popup a la vez).
 * - `cerrar` solo limpia si el incidente cerrado es el seleccionado.
 * - `limpiar` descarta la selección al refrescar los marcadores.
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

/** Subconjunto de `Document` necesario para construir el contenido. */
export interface DocumentoPopup {
  createElement(tagName: string): HTMLElement
}

/**
 * Construye el contenido del tooltip del marcador como un nodo de texto.
 *
 * Leaflet interpreta como HTML el contenido de un tooltip cuando se le pasa un
 * string, por lo que se entrega siempre un elemento con `textContent`: así un
 * título con etiquetas HTML se muestra de forma literal y no se ejecuta.
 */
export function crearContenidoTooltip(
  titulo: string,
  documento: DocumentoPopup,
): HTMLElement {
  const tooltip = documento.createElement('span')
  tooltip.textContent = typeof titulo === 'string' ? titulo : ''
  return tooltip
}

/**
 * Construye el contenido del popup con `textContent` en todos los campos.
 * Conserva el identificador del incidente en `data-incident-id` para consumo
 * futuro desde la capa o desde pruebas.
 */
export function crearContenidoPopup(
  marcador: IncidentMapMarker,
  documento: DocumentoPopup,
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

  return contenedor
}
