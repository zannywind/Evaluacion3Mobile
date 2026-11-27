package com.mpaznotifica.app.data.remote

import com.mpaznotifica.app.model.CanalDespacho
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Contrato del servicio de mensajería (RF-07): una llamada despacha UN mensaje por su canal.
 * [ApiClient] lo instancia con Retrofit contra el backend (POST /mensajes, servicio de correo, SMS
 * y WhatsApp simulado en el servidor). [MensajeriaSimulada] es una implementación local sin red
 * que usan las pruebas JVM del despachador.
 *
 * Un contacto inválido para el canal NO es un error HTTP: la respuesta es 200 con
 * [RespuestaEnvioDto.despachado] en falso y el motivo en [RespuestaEnvioDto.detalle].
 */
interface MensajeriaApi {
    @POST("mensajes")
    suspend fun enviar(@Body mensaje: MensajeDto): RespuestaEnvioDto
}

/** Mensaje hacia UN destinatario; [destinatario] es el correo o el teléfono (ficticio) del canal. */
data class MensajeDto(
    val comunicacionId: String,
    val asunto: String,
    val cuerpo: String,
    val tipo: String,
    val canal: CanalDespacho,
    val destinatario: String,
    val adjunto: String?
)

data class RespuestaEnvioDto(
    val despachado: Boolean = false,
    val detalle: String = ""
)
