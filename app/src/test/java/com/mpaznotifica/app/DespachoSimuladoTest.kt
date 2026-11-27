package com.mpaznotifica.app

import com.mpaznotifica.app.data.MockData
import com.mpaznotifica.app.data.remote.Despachador
import com.mpaznotifica.app.data.remote.MensajeDto
import com.mpaznotifica.app.data.remote.MensajeriaApi
import com.mpaznotifica.app.data.remote.MensajeriaSimulada
import com.mpaznotifica.app.data.remote.MockEmailService
import com.mpaznotifica.app.data.remote.MockSmsService
import com.mpaznotifica.app.data.remote.MockWhatsAppService
import com.mpaznotifica.app.data.remote.RespuestaEnvioDto
import com.mpaznotifica.app.domain.ValidadorContacto
import com.mpaznotifica.app.model.CanalDespacho
import com.mpaznotifica.app.model.Destinatario
import com.mpaznotifica.app.model.EnvioDestinatario
import com.mpaznotifica.app.model.EstadoEnvio
import com.mpaznotifica.app.model.ResumenEnvio
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** RF-07 y RF-08: despacho simulado por canal y estado por destinatario. */
class DespachoSimuladoTest {

    private val ap1 = MockData.apoderados.first { it.id == "AP1" } // correo y teléfono válidos
    private val ap2 = MockData.apoderados.first { it.id == "AP2" }
    private val ap4 = MockData.apoderados.first { it.id == "AP4" } // teléfono incompleto
    private val comunicacion = MockData.comunicados.first { it.id == "C1" }

    private val api = MensajeriaSimulada(listOf(MockEmailService(0), MockSmsService(0), MockWhatsAppService(0)))

    private fun mensaje(canal: CanalDespacho, destinatario: String) = MensajeDto(
        comunicacionId = "X", asunto = "Asunto", cuerpo = "Cuerpo del mensaje", tipo = "CITACION",
        canal = canal, destinatario = destinatario, adjunto = null
    )

    // ------------------------------------------------------------- validación de contacto

    @Test
    fun cadaCanalExigeSuPropioDatoDeContacto() {
        assertTrue(ValidadorContacto.contactoValido(CanalDespacho.EMAIL, "apoderado1@ejemplo.cl"))
        assertFalse(ValidadorContacto.contactoValido(CanalDespacho.EMAIL, "sin-arroba"))
        assertFalse(ValidadorContacto.contactoValido(CanalDespacho.EMAIL, ""))
        assertTrue(ValidadorContacto.contactoValido(CanalDespacho.SMS, "+56900000001"))
        assertTrue(ValidadorContacto.contactoValido(CanalDespacho.WHATSAPP, "+56900000001"))
        assertFalse(ValidadorContacto.contactoValido(CanalDespacho.SMS, "+5690000"))
        assertFalse(ValidadorContacto.contactoValido(CanalDespacho.WHATSAPP, ""))
    }

    @Test
    fun canalesDisponiblesSoloIncluyenLosDeContactoValido() {
        assertEquals(CanalDespacho.entries.toList(), ValidadorContacto.canalesDisponibles(ap1))
        assertEquals(listOf(CanalDespacho.EMAIL), ValidadorContacto.canalesDisponibles(ap4))
    }

    // ------------------------------------------------------------- servicios mock

    @Test
    fun cadaServicioMockDespachaConContactoValido() = runTest {
        CanalDespacho.entries.forEach { canal ->
            val contacto = ValidadorContacto.contactoDe(ap1, canal)
            assertTrue(api.enviar(mensaje(canal, contacto)).despachado)
        }
    }

    @Test
    fun servicioMockFallaConContactoInvalidoYExplicaElMotivo() = runTest {
        val respuesta = api.enviar(mensaje(CanalDespacho.SMS, ap4.telefono))
        assertFalse(respuesta.despachado)
        assertTrue(respuesta.detalle.contains("inválido"))
    }

    @Test
    fun canalSinServicioConfiguradoFalla() = runTest {
        val soloCorreo = MensajeriaSimulada(listOf(MockEmailService(0)))
        assertFalse(soloCorreo.enviar(mensaje(CanalDespacho.SMS, ap1.telefono)).despachado)
    }

    // ------------------------------------------------------------- despachador

    @Test
    fun despachadorRegistraUnEstadoPorDestinatario() = runTest {
        var contador = 0
        val despachador = Despachador(api) { "ENV${++contador}" }
        val resultado = despachador.despachar(
            comunicacion,
            listOf(
                Destinatario(ap1, CanalDespacho.EMAIL),
                Destinatario(ap2, CanalDespacho.WHATSAPP),
                Destinatario(ap4, CanalDespacho.SMS)
            )
        )
        assertEquals(listOf("ENV1", "ENV2", "ENV3"), resultado.map { it.id })
        assertEquals(listOf("AP1", "AP2", "AP4"), resultado.map { it.apoderadoId })
        assertEquals(listOf(CanalDespacho.EMAIL, CanalDespacho.WHATSAPP, CanalDespacho.SMS), resultado.map { it.canal })
        assertEquals(
            listOf(EstadoEnvio.DESPACHADO, EstadoEnvio.DESPACHADO, EstadoEnvio.FALLIDO),
            resultado.map { it.estado }
        )
        assertTrue(resultado.all { it.comunicacionId == comunicacion.id })
        assertTrue(resultado[2].detalle.isNotBlank())
    }

    @Test
    fun despachadorInformaElProgresoPorDestinatario() = runTest {
        val avances = mutableListOf<Pair<Int, Int>>()
        Despachador(api).despachar(
            comunicacion,
            listOf(Destinatario(ap1, CanalDespacho.EMAIL), Destinatario(ap2, CanalDespacho.WHATSAPP))
        ) { procesados, total -> avances.add(procesados to total) }
        assertEquals(listOf(1 to 2, 2 to 2), avances)
    }

    @Test
    fun unaExcepcionDelServicioDejaElEnvioComoFallido() = runTest {
        val apiQueFalla = object : MensajeriaApi {
            override suspend fun enviar(mensaje: MensajeDto): RespuestaEnvioDto = throw IllegalStateException("sin servicio")
        }
        val resultado = Despachador(apiQueFalla).despachar(comunicacion, listOf(Destinatario(ap1, CanalDespacho.EMAIL)))
        assertEquals(EstadoEnvio.FALLIDO, resultado.single().estado)
    }

    @Test
    fun despacharAUnaListaVaciaNoHaceNada() = runTest {
        assertTrue(Despachador(api).despachar(comunicacion, emptyList()).isEmpty())
    }

    // ------------------------------------------------------------- resumen

    @Test
    fun resumenCuentaDespachadosYFallidos() {
        val envios = listOf(
            EnvioDestinatario("1", "C", "AP1", CanalDespacho.EMAIL, EstadoEnvio.DESPACHADO),
            EnvioDestinatario("2", "C", "AP2", CanalDespacho.SMS, EstadoEnvio.FALLIDO),
            EnvioDestinatario("3", "C", "AP3", CanalDespacho.SMS, EstadoEnvio.DESPACHADO)
        )
        val resumen = ResumenEnvio.de(envios, requiereFirmaFisica = true)
        assertEquals(2, resumen.despachados)
        assertEquals(1, resumen.fallidos)
        assertEquals(3, resumen.total)
        assertTrue(resumen.requiereFirmaFisica)
    }
}
