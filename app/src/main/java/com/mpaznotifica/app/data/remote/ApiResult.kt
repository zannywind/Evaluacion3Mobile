package com.mpaznotifica.app.data.remote

/** Causa de un error devuelto por el servidor o por la lectura de su respuesta. */
enum class CausaError {
    /** 400 / 422: datos o regla de negocio rechazados por el servidor. */
    VALIDACION,
    NO_AUTORIZADO,
    PROHIBIDO,
    NO_ENCONTRADO,
    CONFLICTO,
    SERVIDOR,
    RESPUESTA_INVALIDA,
    LOCAL,
    DESCONOCIDA
}

/** Resultado de una llamada a la API, sin excepciones hacia la interfaz. */
sealed interface ApiResult<out T> {

    data class Success<out T>(val datos: T) : ApiResult<T>

    /** El servidor respondió, pero con un error. [campo] es el campo del formulario afectado, si lo hay. */
    data class Error(
        val mensaje: String,
        val causa: CausaError,
        val codigo: Int? = null,
        val campo: String? = null
    ) : ApiResult<Nothing>

    /** No hubo respuesta: sin red, servidor apagado o tiempo agotado. */
    data class SinConexion(
        val mensaje: String = MENSAJE_SIN_CONEXION,
        val tiempoAgotado: Boolean = false
    ) : ApiResult<Nothing>

    companion object {
        const val MENSAJE_SIN_CONEXION = "No se pudo conectar con el servidor. Revisa tu conexión y la dirección del servidor."
        const val MENSAJE_TIEMPO_AGOTADO = "El servidor tardó demasiado en responder. Intenta nuevamente."
    }
}

val <T> ApiResult<T>.esExito: Boolean get() = this is ApiResult.Success

/** Mensaje para mostrar al usuario cuando el resultado no fue exitoso (null si lo fue). */
val ApiResult<*>.mensajeDeError: String?
    get() = when (this) {
        is ApiResult.Success -> null
        is ApiResult.Error -> mensaje
        is ApiResult.SinConexion -> mensaje
    }

/** Transforma el dato de un resultado exitoso; los errores pasan sin cambios. */
inline fun <T, R> ApiResult<T>.mapear(transformar: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transformar(datos))
    is ApiResult.Error -> this
    is ApiResult.SinConexion -> this
}
