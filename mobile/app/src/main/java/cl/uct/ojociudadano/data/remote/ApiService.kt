package cl.uct.ojociudadano.data.remote

import cl.uct.ojociudadano.data.session.TokenSnapshot
import cl.uct.ojociudadano.data.remote.dto.CategoryDto
import cl.uct.ojociudadano.data.remote.dto.CreateIncidentRequestDto
import cl.uct.ojociudadano.data.remote.dto.IncidentDto
import cl.uct.ojociudadano.data.remote.dto.LoginRequestDto
import cl.uct.ojociudadano.data.remote.dto.LoginResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Tag

interface ApiService {

    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: LoginRequestDto
    ): LoginResponseDto

    @GET("api/v1/incidentes")
    suspend fun getIncidents(
        @Query("usuarioReportaId") usuarioReportaId: String? = null,
        @Tag expectedSession: TokenSnapshot? = null
    ): List<IncidentDto>

    @GET("api/v1/incidentes/{id}")
    suspend fun getIncidentById(
        @Path("id") id: String,
        @Tag expectedSession: TokenSnapshot? = null
    ): IncidentDto

    @GET("api/v1/categorias")
    suspend fun getCategories(): List<CategoryDto>

    @POST("api/v1/incidentes")
    suspend fun createIncident(
        @Body request: CreateIncidentRequestDto,
        @Tag expectedSession: TokenSnapshot? = null
    ): IncidentDto
}
