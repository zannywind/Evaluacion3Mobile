package com.mpaznotifica.app.data.remote

import com.mpaznotifica.app.domain.ValidadorContacto
import com.mpaznotifica.app.model.Comunicacion
import com.mpaznotifica.app.model.Destinatario
import com.mpaznotifica.app.model.EnvioDestinatario
import com.mpaznotifica.app.model.EstadoEnvio
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.util.UUID

/**
 * Despacha un comunicado a sus destinatarios a través de [MensajeriaApi] y devuelve un
 * [EnvioDestinatario] por apoderado con el estado resultante (RF-07, RF-08). Un único intento
 * por destinatario: no hay reintentos (supuesto pendiente con el CITT).
 *
 * Si el servicio falla con un error propio, ese destinatario queda FALLIDO con un motivo en
 * español y el despacho continúa. Si el servidor no responde (sin red, tiempo agotado: una
 * [IOException]), los destinatarios restantes quedan FALLIDO de inmediato, sin esperar el plazo
 * de cada uno. No depende de ManejadorErrores: se usa igual en modo local y en modo API.
 */
class Despachador(
    private val api: MensajeriaApi,
    private val generarId: () -> String = { UUID.randomUUID().toString() }
) {

    suspend fun despachar(
        comunicacion: Comunicacion,
        destinatarios: List<Destinatario>,
        onProgreso: (procesados: Int, total: Int) -> Unit = { _, _ -> }
    ): List<EnvioDestinatario> {
        val total = destinatarios.size
        var sinConexion: String? = null
        return destinatarios.mapIndexed { indice, destinatario ->
            val corte = sinConexion
            val respuesta = if (corte != null) {
                RespuestaEnvioDto(false, corte)
            } else {
                try {
                    api.enviar(
                        MensajeDto(
                            comunicacionId = comunicacion.id,
                            asunto = comunicacion.asunto,
                            cuerpo = comunicacion.cuerpo,
                            tipo = comunicacion.tipo.name,
                            canal = destinatario.canal,
                            destinatario = ValidadorContacto.contactoDe(destinatario.apoderado, destinatario.canal),
                            adjunto = comunicacion.adjuntoPdfNombre
                        )
                    )
                } catch (cancelacion: CancellationException) {
                    throw cancelacion
                } catch (error: IOException) {
                    sinConexion = MOTIVO_SIN_CONEXION
                    RespuestaEnvioDto(false, MOTIVO_SIN_CONEXION)
                } catch (error: Exception) {
                    RespuestaEnvioDto(false, MOTIVO_SERVICIO)
                }
            }
            onProgreso(indice + 1, total)
            EnvioDestinatario(
                id = generarId(),
                comunicacionId = comunicacion.id,
                apoderadoId = destinatario.apoderado.id,
                canal = destinatario.canal,
                estado = if (respuesta.despachado) EstadoEnvio.DESPACHADO else EstadoEnvio.FALLIDO,
                detalle = respuesta.detalle
            )
        }
    }

    private companion object {
        const val MOTIVO_SERVICIO = "El servicio no pudo completar el despacho simulado"
        const val MOTIVO_SIN_CONEXION = "No se pudo conectar con el servidor de mensajería"
    }
}
