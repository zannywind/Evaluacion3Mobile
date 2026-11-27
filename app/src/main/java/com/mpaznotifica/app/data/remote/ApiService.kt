package com.mpaznotifica.app.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Rutas del backend (carpeta backend/, ver docs/API.md). Todas son suspend: Retrofit las ejecuta
 * fuera del hilo principal y lanza HttpException ante un 4xx/5xx; [ManejadorErrores] lo convierte
 * en [ApiResult]. Salvo /health y /auth/login, todas exigen el token Bearer que agrega
 * el interceptor de [ApiClient].
 *
 * El despacho de mensajes (POST /mensajes) está en [MensajeriaApi].
 */
interface ApiService {

    @GET("health")
    suspend fun salud(): SaludDto

    // ---- RF-01: autenticación
    @POST("auth/login")
    suspend fun login(@Body datos: LoginRequestDto): LoginResponseDto

    /** El token se pasa explícito: la app ya lo descartó cuando esta llamada se ejecuta. */
    @POST("auth/logout")
    suspend fun logout(@Header("Authorization") autorizacion: String)

    // ---- Datos de referencia (RF-04, RF-13)
    @GET("usuarios")
    suspend fun usuarios(): List<UsuarioDto>

    @GET("talleres")
    suspend fun talleres(): List<TallerDto>

    @GET("estudiantes")
    suspend fun estudiantes(@Query("curso") curso: String?): List<EstudianteDto>

    @GET("apoderados")
    suspend fun apoderados(@Query("curso") curso: String?): List<ApoderadoDto>

    @PATCH("apoderados/{id}/canal")
    suspend fun actualizarCanal(@Path("id") apoderadoId: String, @Body datos: CanalRequestDto): ApoderadoDto

    // ---- RF-09, RF-10, RF-11: bitácora
    @GET("comunicaciones")
    suspend fun comunicaciones(
        @Query("tipo") tipo: String?,
        @Query("emisorId") emisorId: String?,
        @Query("desde") desde: String?,
        @Query("hasta") hasta: String?
    ): List<ComunicacionDto>

    @GET("comunicaciones/{id}")
    suspend fun comunicacion(@Path("id") id: String): ComunicacionRegistradaDto

    @GET("envios")
    suspend fun envios(@Query("comunicacionId") comunicacionId: String?): List<EnvioDto>

    // ---- RF-03 a RF-08, RF-12: emisión
    @POST("comunicaciones")
    suspend fun crearComunicacion(@Body datos: NuevaComunicacionDto): ComunicacionRegistradaDto
}
