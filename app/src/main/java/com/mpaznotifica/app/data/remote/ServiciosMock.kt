package com.mpaznotifica.app.data.remote

import com.mpaznotifica.app.domain.ValidadorContacto
import com.mpaznotifica.app.model.CanalDespacho
import kotlinx.coroutines.delay

/**
 * Servicio mockeado de un canal (RF-07). No existe cliente HTTP ni SDK de mensajería: espera
 * una latencia corta (delay suspende sin bloquear la interfaz) y responde según el dato de
 * contacto. Con un contacto inválido o ausente para el canal, el despacho FALLA.
 */
abstract class ServicioMock(
    val canal: CanalDespacho,
    private val latenciaMs: Long
) {
    suspend fun enviar(mensaje: MensajeDto): RespuestaEnvioDto {
        delay(latenciaMs)
        return if (ValidadorContacto.contactoValido(canal, mensaje.destinatario)) {
            RespuestaEnvioDto(true, "Despachado (simulado) por ${canal.etiqueta}")
        } else {
            RespuestaEnvioDto(false, ValidadorContacto.motivoInvalido(canal))
        }
    }
}

class MockEmailService(latenciaMs: Long = LATENCIA_POR_DEFECTO_MS) :
    ServicioMock(CanalDespacho.EMAIL, latenciaMs)

class MockSmsService(latenciaMs: Long = LATENCIA_POR_DEFECTO_MS) :
    ServicioMock(CanalDespacho.SMS, latenciaMs)

class MockWhatsAppService(latenciaMs: Long = LATENCIA_POR_DEFECTO_MS) :
    ServicioMock(CanalDespacho.WHATSAPP, latenciaMs)

private const val LATENCIA_POR_DEFECTO_MS = 250L

/** Enruta cada mensaje al servicio mock de su canal. */
class MensajeriaSimulada(
    servicios: List<ServicioMock> = listOf(MockEmailService(), MockSmsService(), MockWhatsAppService())
) : MensajeriaApi {

    private val porCanal: Map<CanalDespacho, ServicioMock> = servicios.associateBy { it.canal }

    override suspend fun enviar(mensaje: MensajeDto): RespuestaEnvioDto =
        porCanal[mensaje.canal]?.enviar(mensaje)
            ?: RespuestaEnvioDto(false, "Servicio no disponible para ${mensaje.canal.etiqueta}")
}
