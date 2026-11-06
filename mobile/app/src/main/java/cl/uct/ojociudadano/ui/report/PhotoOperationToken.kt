package cl.uct.ojociudadano.ui.report

/**
 * Identifica una operación fotográfica concreta (cámara o galería).
 *
 * Cada vez que el formulario inicia una operación se genera un token nuevo.
 * Solo la operación cuyo token sigue siendo el activo puede publicar su
 * resultado; los callbacks de operaciones anteriores se descartan para evitar
 * que una importación lenta sobrescriba una selección más reciente.
 */
typealias PhotoOperationToken = String
