package com.mpaznotifica.app.data.remote

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import retrofit2.Retrofit
import java.io.IOException

/*
 * EXPERIENCIA 3 - Parte 2 (TODO E3-1): cliente HTTP.
 *
 * Esta clase fabrica las implementaciones Retrofit de [ApiService] y [MensajeriaApi] contra la
 * dirección activa de [ConfiguracionServidor]. Solo se usa en modo servidor.
 */

/**
 * TODO(E3-1a): agrega el token Bearer de la sesión a cada petición.
 * Archivo: este (clase [InterceptorToken]).
 * Criterio de aceptación: si [SesionApi.token] no es null y la petición NO trae ya la cabecera
 * Authorization, la petición sale con `Authorization: Bearer <token>`; sin token sale intacta.
 * Debe terminar llamando a `chain.proceed(...)`.
 */
class InterceptorToken(private val sesion: SesionApi) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        TODO("E3-1a: agregar la cabecera Authorization: Bearer <token> cuando exista sesión")
    }
}

/**
 * Última defensa del tráfico sin cifrar (ya implementada): Android no permite expresar rangos de IP
 * en network_security_config, así que aquí se rechaza cualquier http:// hacia un host que no sea
 * local (ver [UrlServidor.permiteCleartext]).
 */
class GuardiaCleartext : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val peticion = chain.request()
        if (!peticion.isHttps && !UrlServidor.permiteCleartext(peticion.url.host)) {
            throw IOException("Tráfico sin cifrar no permitido hacia ${peticion.url.host}")
        }
        return chain.proceed(peticion)
    }
}

/**
 * Ya implementada: cabecera que pide a los túneles de VS Code (Port Forwarding) no mostrar su
 * página de aviso intermedia, y pide JSON al servidor.
 */
class InterceptorTunel : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val peticion: Request = chain.request().newBuilder()
            .header("X-Tunnel-Skip-AntiPhishing-Page", "true")
            .header("Accept", "application/json")
            .build()
        return chain.proceed(peticion)
    }
}

/**
 * Crea los clientes Retrofit contra la dirección activa de [configuracion]. Si el usuario cambia la
 * dirección (diálogo "Servidor"), la siguiente llamada debe reconstruir el cliente: no hace falta
 * reiniciar la app.
 */
class ApiClient(
    private val configuracion: ConfiguracionServidor,
    private val sesion: SesionApi,
    private val interceptoresDepuracion: List<Interceptor> = RegistroHttp.interceptores()
) {

    /**
     * TODO(E3-1d): entrega el [ApiService] para la URL activa.
     * Archivo: este. Criterio: usa `configuracion.urlBase()`; reutiliza la misma instancia mientras
     * la URL no cambie y construye una nueva (con [crearRetrofit] y [clienteHttp]) si cambió.
     */
    fun api(): ApiService =
        TODO("E3-1d: crear/reutilizar Retrofit.create(ApiService::class.java) para configuracion.urlBase()")

    /** TODO(E3-1d): igual que [api], para [MensajeriaApi] (POST /mensajes). Debe compartir el cliente HTTP. */
    fun mensajeria(): MensajeriaApi =
        TODO("E3-1d: Retrofit.create(MensajeriaApi::class.java) con el mismo Retrofit que api()")

    /**
     * TODO(E3-9c): "Probar conexión". GET /health a una dirección candidata, sin token y con plazo
     * corto (6 s).
     * Archivo: este. Criterios: si `UrlServidor.validar(urlCandidata)` da error, devuelve
     * `ApiResult.Error(mensaje, CausaError.VALIDACION, campo = "url")` sin llamar a la red; si la
     * respuesta no trae `estado == "ok"`, devuelve error de respuesta inválida (lanza
     * [RespuestaInvalidaException] dentro de [ManejadorErrores.llamar]); en los demás casos
     * devuelve el [SaludDto].
     */
    suspend fun probarConexion(urlCandidata: String): ApiResult<SaludDto> =
        TODO("E3-9c: validar la URL, crear un Retrofit con plazo de 6 s sin token y llamar a salud()")

    /**
     * TODO(E3-1b): construye el [OkHttpClient].
     * Archivo: este. Criterios: connectTimeout = min(plazoSegundos, 10) s; readTimeout y
     * writeTimeout = plazoSegundos; callTimeout = plazoSegundos + 10 s; interceptores en este
     * orden: [GuardiaCleartext], [InterceptorTunel], [InterceptorToken] (solo si [conToken]) y
     * luego todos los de [interceptoresDepuracion].
     */
    private fun clienteHttp(conToken: Boolean, plazoSegundos: Long = 20): OkHttpClient =
        TODO("E3-1b: OkHttpClient.Builder con timeouts e interceptores")

    companion object {
        /**
         * TODO(E3-1c): construye el [Retrofit].
         * Archivo: este. Criterios: `baseUrl(urlBase)`, `client(cliente)` y conversor Gson
         * (`GsonConverterFactory.create()`).
         */
        fun crearRetrofit(urlBase: String, cliente: OkHttpClient): Retrofit =
            TODO("E3-1c: Retrofit.Builder().baseUrl(...).client(...).addConverterFactory(GsonConverterFactory.create()).build()")
    }
}
