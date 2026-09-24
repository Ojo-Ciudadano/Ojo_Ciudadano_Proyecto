package cl.uct.ojociudadano.data.model

data class IncidentLocation(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val referenceAddress: String,
    val commune: String,
    val region: String,
    val sector: String
)
