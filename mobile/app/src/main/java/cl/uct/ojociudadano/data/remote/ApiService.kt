package cl.uct.ojociudadano.data.remote

import cl.uct.ojociudadano.data.session.TokenSnapshot
import cl.uct.ojociudadano.data.remote.dto.CategoryDto
import cl.uct.ojociudadano.data.remote.dto.CreateIncidentRequestDto
import cl.uct.ojociudadano.data.remote.dto.IncidentDto
import cl.uct.ojociudadano.data.remote.dto.LoginRequestDto
import cl.uct.ojociudadano.data.remote.dto.LoginResponseDto
import cl.uct.ojociudadano.data.remote.dto.NotificationDto
import cl.uct.ojociudadano.data.remote.dto.EvidenceDto
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import cl.uct.ojociudadano.data.remote.dto.UpdateIncidentStatusRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Tag
import retrofit2.http.Streaming
import retrofit2.http.PATCH
import cl.uct.ojociudadano.data.remote.dto.RegisterRequestDto
import cl.uct.ojociudadano.data.remote.dto.RegisterResponseDto
import cl.uct.ojociudadano.data.remote.dto.PaginatedResponseDto
import cl.uct.ojociudadano.data.remote.dto.UsuarioDto
import cl.uct.ojociudadano.data.remote.dto.HistorialEstadoDto
import cl.uct.ojociudadano.data.remote.dto.AsignacionDto
import cl.uct.ojociudadano.data.remote.dto.AssignResponsibleRequestDto
import cl.uct.ojociudadano.data.remote.dto.UpdatePriorityRequestDto

interface ApiService {

    @PATCH("api/v1/incidentes/{id}/estado")
    suspend fun updateIncidentStatus(
        @Path("id") incidentId: String,
        @Body request: UpdateIncidentStatusRequestDto,
        @Tag expectedSession: TokenSnapshot? = null
    ): IncidentDto

    @POST("api/v1/auth/register")
    suspend fun register(
        @Body request: RegisterRequestDto
    ): RegisterResponseDto

    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: LoginRequestDto
    ): LoginResponseDto

    @GET("api/v1/incidentes")
    suspend fun getIncidents(
        @Query("usuarioReportaId") usuarioReportaId: String? = null,
        @Query("estadoId") estadoId: String? = null,
        @Query("categoriaId") categoriaId: String? = null,
        @Query("comuna") comuna: String? = null,
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

    @GET("api/v1/notificaciones")
    suspend fun getNotifications(
        @Tag expectedSession: TokenSnapshot? = null
    ): List<NotificationDto>

    @GET("api/v1/usuarios")
    suspend fun getUsuarios(
        @Query("page") page: Int? = 1,
        @Query("limit") limit: Int? = 20,
        @Query("rol") rol: String? = null,
        @Tag expectedSession: TokenSnapshot? = null
    ): PaginatedResponseDto<UsuarioDto>

    @GET("api/v1/usuarios/me")
    suspend fun getMyProfile(
        @Tag expectedSession: TokenSnapshot? = null
    ): UsuarioDto

    @GET("api/v1/incidentes/{id}/historial")
    suspend fun getIncidentHistory(
        @Path("id") incidentId: String,
        @Tag expectedSession: TokenSnapshot? = null
    ): List<HistorialEstadoDto>

    @PATCH("api/v1/incidentes/{id}/asignacion")
    suspend fun assignResponsible(
        @Path("id") incidentId: String,
        @Body request: AssignResponsibleRequestDto,
        @Tag expectedSession: TokenSnapshot? = null
    ): AsignacionDto

    @PATCH("api/v1/incidentes/{id}/prioridad")
    suspend fun updatePriority(
        @Path("id") incidentId: String,
        @Body request: UpdatePriorityRequestDto,
        @Tag expectedSession: TokenSnapshot? = null
    ): IncidentDto
}
