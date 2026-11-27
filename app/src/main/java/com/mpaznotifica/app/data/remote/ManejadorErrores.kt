package com.mpaznotifica.app.data.remote

import com.google.gson.Gson
import com.google.gson.JsonParseException
import retrofit2.HttpException
import java.io.IOException

/** La respuesta del servidor llegó, pero le faltan datos o trae valores que la app no conoce. */
class RespuestaInvalidaException(mensaje: String) : Exception(mensaje)

/**
 * EXPERIENCIA 3 - Parte 2 (TODO E3-2 y E3-3): convierte excepciones de red en [ApiResult] con
 * mensajes en español para el usuario. La interfaz nunca debe ver una excepción.
 *
 * Formato de los errores del backend (docs/API.md): cuerpo JSON `{ "error": "...", "campo": "..." }`.
 */
object ManejadorErrores {

    const val MENSAJE_RESPUESTA_INVALIDA = "El servidor respondió con un formato inesperado. Intenta nuevamente."
    const val MENSAJE_SERVIDOR = "El servidor tuvo un problema. Intenta nuevamente en unos minutos."
    const val MENSAJE_INESPERADO = "Ocurrió un error inesperado. Intenta nuevamente."

    private val gson = Gson()

    /**
     * TODO(E3-2): ejecuta [bloque] y lo envuelve en un [ApiResult].
     * Archivo: este. Criterios de aceptación:
     *  - Sin excepción -> `ApiResult.Success(valor)`.
     *  - CancellationException -> se vuelve a lanzar (NUNCA se traga la cancelación de corrutinas).
     *  - Cualquier otra excepción -> `desdeExcepcion(e)`.
     */
    suspend fun <T> llamar(bloque: suspend () -> T): ApiResult<T> =
        TODO("E3-2: try { Success(bloque()) } catch (CancellationException) { throw } catch (Exception) { desdeExcepcion(e) }")

    /**
     * TODO(E3-3a): traduce una excepción a [ApiResult]. Criterios de aceptación:
     *  - [HttpException] -> `desdeHttp(codigo, cuerpoDe(e))`.
     *  - [java.net.SocketTimeoutException] -> `ApiResult.SinConexion(ApiResult.MENSAJE_TIEMPO_AGOTADO, tiempoAgotado = true)`.
     *    (OJO: es subclase de IOException; debe evaluarse ANTES.)
     *  - [IOException] -> `ApiResult.SinConexion()` (sin red, servidor apagado, host inalcanzable).
     *  - [JsonParseException] y [RespuestaInvalidaException] -> `ApiResult.Error(MENSAJE_RESPUESTA_INVALIDA, CausaError.RESPUESTA_INVALIDA)`.
     *  - Cualquier otra -> `ApiResult.Error(MENSAJE_INESPERADO, CausaError.DESCONOCIDA)`.
     */
    fun desdeExcepcion(e: Throwable): ApiResult<Nothing> =
        TODO("E3-3a: when (e) { is HttpException -> ...; is SocketTimeoutException -> ...; is IOException -> ...; ... }")

    /**
     * TODO(E3-3b): traduce un código HTTP (y el cuerpo de error, si lo hay) a [ApiResult.Error].
     * Si el cuerpo es JSON `{error, campo}` se usa `error` como mensaje y `campo` como campo del
     * formulario afectado; si no es JSON válido o viene vacío se usa el mensaje por defecto.
     *
     *  | Código | CausaError     | Mensaje por defecto (si el servidor no trae `error`)           |
     *  |--------|----------------|-----------------------------------------------------------------|
     *  | 400    | VALIDACION     | Los datos enviados no son válidos.                              |
     *  | 422    | VALIDACION     | El servidor no pudo procesar los datos enviados.                |
     *  | 401    | NO_AUTORIZADO  | Tu sesión expiró. Ingresa nuevamente.                           |
     *  | 403    | PROHIBIDO      | Tu perfil no tiene permiso para realizar esta acción.           |
     *  | 404    | NO_ENCONTRADO  | No se encontró lo solicitado en el servidor.                    |
     *  | 405    | PROHIBIDO      | Esa operación no está permitida.                                |
     *  | 409    | CONFLICTO      | La operación entra en conflicto con datos ya registrados.       |
     *  | 5xx    | SERVIDOR       | [MENSAJE_SERVIDOR] (siempre, sin mostrar el detalle del servidor) |
     *  | otro   | DESCONOCIDA    | El servidor respondió con un error (<código>).                  |
     *
     * El resultado debe incluir `codigo` y `campo` (en 5xx, `campo` = null).
     */
    fun desdeHttp(codigo: Int, cuerpo: String?): ApiResult.Error =
        TODO("E3-3b: aplicar la tabla de códigos de arriba; interpretar el cuerpo con ErrorDto")

    // ---- Ayudas ya implementadas -----------------------------------------------------------

    /** Cuerpo de error del servidor: `{ "error": "...", "campo": "..." }`. Útil para E3-3b. */
    class ErrorDto(val error: String? = null, val campo: String? = null)

    /** Lee el cuerpo de error de una [HttpException] (null si no se puede leer). Útil para E3-3a. */
    fun cuerpoDe(e: HttpException): String? =
        try {
            e.response()?.errorBody()?.string()
        } catch (ignorada: IOException) {
            null
        }

    /** Interpreta un cuerpo de error JSON (null si viene vacío o mal formado). Útil para E3-3b. */
    fun interpretarCuerpo(cuerpo: String?): ErrorDto? =
        try {
            if (cuerpo.isNullOrBlank()) null else gson.fromJson(cuerpo, ErrorDto::class.java)
        } catch (e: JsonParseException) {
            null
        }
}
