package cl.uct.ojociudadano.ui.report.photo

/**
 * Mensajes que el formulario muestra cuando una evidencia local no puede
 * previsualizarse.
 *
 * Se centralizan aquí para distinguir entre una referencia que dejó de existir
 * y un archivo que sigue disponible pero no puede decodificarse.
 */
object PhotoPreviewErrors {

    const val UNAVAILABLE =
        "Una fotografía ya no está disponible en el dispositivo; elimínala o vuelve a seleccionarla"

    const val RENDER_FAILED =
        "No fue posible mostrar una fotografía; puedes eliminarla del reporte"
}
