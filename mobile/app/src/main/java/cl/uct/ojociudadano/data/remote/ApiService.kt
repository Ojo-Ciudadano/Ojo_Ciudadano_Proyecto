package cl.uct.ojociudadano.data.remote

import cl.uct.ojociudadano.data.session.TokenSnapshot
import cl.uct.ojociudadano.data.remote.dto.CategoryDto
import cl.uct.ojociudadano.data.remote.dto.CreateIncidentRequestDto
import cl.uct.ojociudadano.data.remote.dto.IncidentDto
import cl.uct.ojociudadano.data.remote.dto.LoginRequestDto
import cl.uct.ojociudadano.data.remote.dto.LoginResponseDto
import cl.uct.ojociudadano.data.remote.dto.EvidenceDto
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Tag
import retrofit2.http.Streaming

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

    @Multipart
    @POST("api/v1/incidentes/{incidenteId}/evidencias")
    suspend fun uploadEvidence(
        @Path("incidenteId") incidenteId: String,
        @Part file: MultipartBody.Part,
        @Tag expectedSession: TokenSnapshot? = null
    ): EvidenceDto

    @GET("api/v1/incidentes/{incidenteId}/evidencias")
    suspend fun getEvidencesByIncident(
        @Path("incidenteId") incidenteId: String,
        @Tag expectedSession: TokenSnapshot? = null
    ): List<EvidenceDto>

    @GET("api/v1/evidencias/{id}")
    suspend fun getEvidenceById(
        @Path("id") id: String,
        @Tag expectedSession: TokenSnapshot? = null
    ): EvidenceDto

    @Streaming
    @GET("api/v1/evidencias/{id}/archivo")
    suspend fun downloadEvidenceFile(
        @Path("id") id: String,
        @Tag expectedSession: TokenSnapshot? = null
    ): ResponseBody
}
