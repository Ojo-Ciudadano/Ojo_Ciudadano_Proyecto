package cl.uct.ojociudadano.data.model

data class CreateIncidentInput(
    val title: String,
    val description: String,
    val categoryId: String,
    val location: CreateIncidentLocationInput
)

data class CreateIncidentLocationInput(
    val latitude: Double,
    val longitude: Double,
    val referenceAddress: String,
    val commune: String,
    val region: String,
    val sector: String
)
