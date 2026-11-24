package cl.uct.ojociudadano.ui.report

/**
 * Mensajes no bloqueantes del autocompletado de ubicación.
 *
 * Se muestran como ayuda junto a los campos editables y nunca se agregan a las
 * validaciones que impiden crear el reporte.
 */
object CreateReportAddressAutofillErrors {

    const val NO_RESULTS =
        "No encontramos una dirección para esta ubicación. Puedes escribirla manualmente."

    const val UNAVAILABLE =
        "No fue posible completar la dirección automáticamente. Puedes escribirla manualmente."
}
