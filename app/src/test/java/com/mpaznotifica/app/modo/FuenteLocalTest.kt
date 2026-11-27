package com.mpaznotifica.app.modo

import com.mpaznotifica.app.data.MockData
import com.mpaznotifica.app.data.remote.ApiResult
import com.mpaznotifica.app.data.remote.CausaError
import com.mpaznotifica.app.data.remote.Despachador
import com.mpaznotifica.app.data.remote.MensajeriaSimulada
import com.mpaznotifica.app.data.remote.MockEmailService
import com.mpaznotifica.app.data.remote.MockSmsService
import com.mpaznotifica.app.data.remote.MockWhatsAppService
import com.mpaznotifica.app.data.repository.FuenteLocal
import com.mpaznotifica.app.domain.ReglasComunicado
import com.mpaznotifica.app.model.AlcanceEnvio
import com.mpaznotifica.app.model.CanalDespacho
import com.mpaznotifica.app.model.Comunicacion
import com.mpaznotifica.app.model.Destinatario
import com.mpaznotifica.app.model.EnvioDestinatario
import com.mpaznotifica.app.model.EstadoEnvio
import com.mpaznotifica.app.model.Perfil
import com.mpaznotifica.app.model.TipoComunicacion
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Modo local: login por hash, reglas de negocio con escritura en la base local y restablecer. */
class FuenteLocalTest {

    private val almacen = AlmacenFalso()
    private val fuente = FuenteLocal(
        almacen,
        MensajeriaSimulada(listOf(MockEmailService(0), MockSmsService(0), MockWhatsAppService(0)))
    )

    private val inspectoria = MockData.usuarios.first { it.perfil == Perfil.INSPECTORIA_GENERAL }
    private val profesor = MockData.usuarios.first { it.id == "U2" } // jefe de 4° Básico A

    private fun comunicacion(
        id: String = "NUEVO-1",
        autor: com.mpaznotifica.app.model.Usuario = profesor,
        asunto: String = "Reunión de apoderados",
        cuerpo: String = "Se cita a reunión el viernes a las 18:00 hrs.",
        tipo: TipoComunicacion = TipoComunicacion.CITACION,
        alcance: AlcanceEnvio = AlcanceEnvio.CURSO,
        destino: String? = "4° Básico A",
        firma: Boolean = false
    ) = Comunicacion(
        id = id, asunto = asunto, cuerpo = ReglasComunicado.cuerpoFinal(cuerpo, firma), tipo = tipo,
        alcance = alcance, destinoReferencia = destino, requiereFirmaFisica = firma,
        autorId = autor.id, autorNombre = autor.nombre, fechaHora = "2026-11-20 09:00:00"
    )

    /** Envíos de los apoderados del curso 4° A (AP1, AP2, AP4) por su canal, como los dejaría el despachador. */
    private fun envios(c: Comunicacion, canalFijo: CanalDespacho? = null) =
        listOf("AP1", "AP2", "AP4").map { id ->
            val ap = MockData.apoderados.first { it.id == id }
            EnvioDestinatario("EV-$id", c.id, id, canalFijo ?: ap.canalPreferente, EstadoEnvio.DESPACHADO, "ok")
        }

    // ------------------------------------------------------------------ login (RF-01)

    @Test
    fun loginLocalConClaveCorrectaDevuelveUsuarioYPerfil() = runTest {
        val r = fuente.iniciarSesion("inspectoria@mpaznotifica.cl", "inspectoria123") as ApiResult.Success
        assertEquals(Perfil.INSPECTORIA_GENERAL, r.datos.usuario.perfil)
        assertEquals(FuenteLocal.TOKEN_LOCAL, r.datos.token)
    }

    @Test
    fun loginLocalAceptaElCorreoConEspaciosYMayusculas() = runTest {
        val r = fuente.iniciarSesion("  PROFESOR.4basicoA@mpaznotifica.cl ", "profesor123") as ApiResult.Success
        assertEquals("4° Básico A", r.datos.usuario.cursoAsignado)
    }

    @Test
    fun loginLocalConClaveIncorrectaEsNoAutorizado() = runTest {
        val r = fuente.iniciarSesion("inspectoria@mpaznotifica.cl", "otra-clave") as ApiResult.Error
        assertEquals(CausaError.NO_AUTORIZADO, r.causa)
        assertEquals("Correo o clave incorrectos", r.mensaje)
    }

    @Test
    fun loginLocalConUsuarioInexistenteEsNoAutorizado() = runTest {
        val r = fuente.iniciarSesion("nadie@mpaznotifica.cl", "inspectoria123") as ApiResult.Error
        assertEquals(CausaError.NO_AUTORIZADO, r.causa)
    }

    @Test
    fun sincronizarEnModoLocalNoHaceNadaYTieneExito() = runTest {
        assertTrue(fuente.sincronizar() is ApiResult.Success)
    }

    // ------------------------------------------------------------------ envío (RF-03 a RF-12, RN-01, RN-02)

    @Test
    fun registrarEnvioValidoLoGuardaEnLaBitacoraLocal() = runTest {
        val c = comunicacion()
        val r = fuente.registrarEnvio(c, "4° Básico A", envios(c)) as ApiResult.Success

        assertEquals(3, r.datos.resumen.total)
        assertTrue(almacen.comunicacionesEstado.value.any { it.id == "NUEVO-1" })
        assertEquals(3, almacen.enviosEstado.value.count { it.comunicacionId == "NUEVO-1" })
    }

    @Test
    fun elProfesorNoPuedeEnviarAColegioCompleto() = runTest {
        val c = comunicacion(alcance = AlcanceEnvio.COLEGIO_COMPLETO, destino = null)
        val r = fuente.registrarEnvio(c, null, emptyList()) as ApiResult.Error

        assertEquals(CausaError.PROHIBIDO, r.causa)
        assertEquals(ReglasComunicado.MSG_SOLO_CURSO, r.mensaje)
        assertFalse(almacen.comunicacionesEstado.value.any { it.id == "NUEVO-1" })
    }

    @Test
    fun elProfesorNoPuedeEnviarAOtroCurso() = runTest {
        val c = comunicacion(destino = "2° Básico A")
        val r = fuente.registrarEnvio(c, "2° Básico A", emptyList()) as ApiResult.Error

        assertEquals(CausaError.PROHIBIDO, r.causa)
        assertEquals("destino", r.campo)
    }

    @Test
    fun elProfesorNoPuedeUsarUnTipoNoPermitido() = runTest {
        val c = comunicacion(tipo = TipoComunicacion.INSTITUCIONAL)
        val r = fuente.registrarEnvio(c, "4° Básico A", envios(c)) as ApiResult.Error

        assertEquals(CausaError.PROHIBIDO, r.causa)
        assertEquals(ReglasComunicado.MSG_TIPO_NO_DISPONIBLE, r.mensaje)
    }

    @Test
    fun unAsuntoVacioEsRechazadoYNoSeGuarda() = runTest {
        val c = comunicacion(asunto = "  ")
        val r = fuente.registrarEnvio(c, "4° Básico A", envios(c)) as ApiResult.Error

        assertEquals(CausaError.VALIDACION, r.causa)
        assertEquals("asunto", r.campo)
        assertEquals(MockData.comunicados.size, almacen.comunicacionesEstado.value.size)
    }

    @Test
    fun inspectoriaSiPuedeEnviarAColegioCompleto() = runTest {
        val c = comunicacion(
            autor = inspectoria, tipo = TipoComunicacion.INSTITUCIONAL,
            alcance = AlcanceEnvio.COLEGIO_COMPLETO, destino = null
        )
        val envios = MockData.apoderados.map {
            EnvioDestinatario("EV-${it.id}", c.id, it.id, it.canalPreferente, EstadoEnvio.DESPACHADO, "ok")
        }

        assertTrue(fuente.registrarEnvio(c, null, envios) is ApiResult.Success)
        assertEquals(MockData.apoderados.size, almacen.enviosEstado.value.count { it.comunicacionId == c.id })
    }

    @Test
    fun unIdentificadorRepetidoEsConflictoYNoDuplicaElRegistro() = runTest {
        val c = comunicacion(id = "C1")
        val r = fuente.registrarEnvio(c, "4° Básico A", envios(c)) as ApiResult.Error

        assertEquals(CausaError.CONFLICTO, r.causa)
        assertEquals("id", r.campo)
        assertEquals(1, almacen.comunicacionesEstado.value.count { it.id == "C1" })
    }

    @Test
    fun losEnviosDeOtroConjuntoDeDestinatariosSonRechazados() = runTest {
        val c = comunicacion()
        val r = fuente.registrarEnvio(c, "4° Básico A", envios(c).take(2)) as ApiResult.Error

        assertEquals(CausaError.VALIDACION, r.causa)
        assertEquals("envios", r.campo)
    }

    @Test
    fun laAutorizacionConFirmaFisicaExigeElCanalCorreoEnTodosLosEnvios() = runTest {
        val sinCorreo = comunicacion(firma = true)
        val rechazado = fuente.registrarEnvio(sinCorreo, "4° Básico A", envios(sinCorreo, CanalDespacho.SMS))
        assertTrue(rechazado is ApiResult.Error)

        val porCorreo = comunicacion(id = "NUEVO-2", firma = true)
        val ok = fuente.registrarEnvio(porCorreo, "4° Básico A", envios(porCorreo, CanalDespacho.EMAIL)) as ApiResult.Success
        assertTrue(ok.datos.resumen.requiereFirmaFisica)
        assertTrue(ok.datos.comunicacion.cuerpo.endsWith(ReglasComunicado.AVISO_FIRMA_FISICA))
    }

    @Test
    fun siRoomFallaAlGuardarSeInformaComoErrorLocal() = runTest {
        almacen.fallarAlGuardar = true
        val c = comunicacion()
        val r = fuente.registrarEnvio(c, "4° Básico A", envios(c)) as ApiResult.Error

        assertEquals(CausaError.LOCAL, r.causa)
    }

    @Test
    fun elDespachoLocalSimuladoMarcaFallidoUnContactoInvalidoYSeRegistraAsi() = runTest {
        val c = comunicacion()
        val destinatarios = listOf("AP1", "AP2", "AP4").map {
            val ap = MockData.apoderados.first { a -> a.id == it }
            Destinatario(ap, ReglasComunicado.canalDe(ap, false))
        }
        val resultados = Despachador(fuente.mensajeria()).despachar(c, destinatarios)
        // AP4 tiene el teléfono incompleto: su SMS falla, los otros dos se despachan.
        assertEquals(EstadoEnvio.FALLIDO, resultados.first { it.apoderadoId == "AP4" }.estado)

        val r = fuente.registrarEnvio(c, "4° Básico A", resultados) as ApiResult.Success
        assertEquals(2, r.datos.resumen.despachados)
        assertEquals(1, r.datos.resumen.fallidos)
    }

    // ------------------------------------------------------------------ canal preferente (RF-13)

    @Test
    fun inspectoriaCambiaElCanalPreferenteYQuedaGuardado() = runTest {
        val r = fuente.actualizarCanalPreferente(inspectoria, "AP1", CanalDespacho.WHATSAPP) as ApiResult.Success

        assertEquals(CanalDespacho.WHATSAPP, r.datos.canalPreferente)
        assertEquals(CanalDespacho.WHATSAPP, almacen.apoderadosEstado.value.first { it.id == "AP1" }.canalPreferente)
    }

    @Test
    fun elProfesorNoPuedeCambiarElCanalPreferente() = runTest {
        val r = fuente.actualizarCanalPreferente(profesor, "AP1", CanalDespacho.SMS) as ApiResult.Error

        assertEquals(CausaError.PROHIBIDO, r.causa)
        assertEquals(CanalDespacho.EMAIL, almacen.apoderadosEstado.value.first { it.id == "AP1" }.canalPreferente)
    }

    @Test
    fun noSePuedeElegirUnCanalSinDatoDeContactoValido() = runTest {
        val r = fuente.actualizarCanalPreferente(inspectoria, "AP4", CanalDespacho.WHATSAPP) as ApiResult.Error

        assertEquals(CausaError.VALIDACION, r.causa)
        assertEquals("canal", r.campo)
        assertEquals(CanalDespacho.SMS, almacen.apoderadosEstado.value.first { it.id == "AP4" }.canalPreferente)
    }

    @Test
    fun cambiarElCanalDeUnApoderadoInexistenteEsNoEncontrado() = runTest {
        val r = fuente.actualizarCanalPreferente(inspectoria, "AP99", CanalDespacho.EMAIL) as ApiResult.Error
        assertEquals(CausaError.NO_ENCONTRADO, r.causa)
    }

    @Test
    fun sinUsuarioActivoNoSePuedeCambiarElCanal() = runTest {
        val r = fuente.actualizarCanalPreferente(null, "AP1", CanalDespacho.SMS) as ApiResult.Error
        assertEquals(CausaError.NO_AUTORIZADO, r.causa)
    }

    // ------------------------------------------------------------------ restablecer

    @Test
    fun restablecerDejaLosDatosDeEjemploYBorraLoCreadoEnModoLocal() = runTest {
        val c = comunicacion()
        fuente.registrarEnvio(c, "4° Básico A", envios(c))
        fuente.actualizarCanalPreferente(inspectoria, "AP1", CanalDespacho.SMS)
        assertEquals(MockData.comunicados.size + 1, fuente.comunicaciones.first().size)

        assertTrue(fuente.restablecer() is ApiResult.Success)

        assertEquals(MockData.comunicados.map { it.id }, fuente.comunicaciones.first().map { it.id })
        assertEquals(CanalDespacho.EMAIL, fuente.apoderados.first().first { it.id == "AP1" }.canalPreferente)
        assertNotNull(fuente.buscarUsuario("inspectoria@mpaznotifica.cl"))
    }
}

/** Fuente "servidor" mínima para la prueba de independencia: otra base en memoria, sin red. */
class FuenteLocalComoServidor(almacen: AlmacenFalso) :
    com.mpaznotifica.app.data.repository.FuenteDatos by FuenteLocal(almacen) {
    override val esLocal: Boolean = false
}
