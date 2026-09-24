package cl.uct.ojociudadano.data.remote

import cl.uct.ojociudadano.data.remote.dto.CategoryDto
import cl.uct.ojociudadano.data.remote.dto.CreateIncidentRequestDto
import cl.uct.ojociudadano.data.remote.dto.IncidentDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @GET("api/v1/incidentes")
    suspend fun getIncidents(
        @Query("usuarioReportaId") usuarioReportaId: String? = null
    ): List<IncidentDto>

    @GET("api/v1/incidentes/{id}")
    suspend fun getIncidentById(
        @Path("id") id: String
    ): IncidentDto

    @GET("api/v1/categorias")
    suspend fun getCategories(): List<CategoryDto>

    @POST("api/v1/incidentes")
    suspend fun createIncident(
        @Body request: CreateIncidentRequestDto
    ): IncidentDto
}
