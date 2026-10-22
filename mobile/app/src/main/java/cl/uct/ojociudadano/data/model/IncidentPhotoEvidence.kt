package cl.uct.ojociudadano.data.model

/**
 * Referencia local a una fotografía capturada como evidencia de un incidente.
 *
 * Solo existe en el dispositivo mientras se completa el formulario: no se
 * persiste de forma definitiva ni se envía al backend en esta tarea. La
 * metadata replica los campos de la evidencia remota para que el flujo de
 * creación pueda reutilizarla cuando se implemente la subida a Supabase Storage.
 */
data class IncidentPhotoEvidence(
    val localUri: String,
    val fileName: String,
    val mimeType: String = "image/jpeg",
    val sizeBytes: Long,
    val capturedAt: Long
)
