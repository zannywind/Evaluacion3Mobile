package com.mpaznotifica.app.modo

import com.mpaznotifica.app.data.MockData
import com.mpaznotifica.app.data.remote.ApiResult
import com.mpaznotifica.app.data.repository.FuenteLocal
import com.mpaznotifica.app.data.repository.ModoEnMemoria
import com.mpaznotifica.app.data.repository.SelectorFuente
import com.mpaznotifica.app.model.CanalDespacho
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Selector de fuente: elige Room (modo local) o API + caché (modo servidor) según la preferencia,
 * en cada operación, y cambia en caliente.
 */
class SelectorFuenteTest {

    private val local = FuenteFalsa("local", esLocal = true)
    private val servidor = FuenteFalsa("servidor", esLocal = false)

    private fun selector(inicialLocal: Boolean) =
        SelectorFuente(ModoEnMemoria(inicialLocal), local, servidor)

    @Test
    fun conPreferenciaLocalLasOperacionesVanALaFuenteLocal() = runTest {
        val s = selector(inicialLocal = true)

        s.iniciarSesion("inspectoria@mpaznotifica.cl", "inspectoria123")
        s.sincronizar()

        assertEquals(listOf("login", "sincronizar"), local.llamadas)
        assertTrue(servidor.llamadas.isEmpty())
        assertTrue(s.esLocal)
    }

    @Test
    fun conPreferenciaServidorLasOperacionesVanALaFuenteDelServidor() = runTest {
        val s = selector(inicialLocal = false)

        s.iniciarSesion("inspectoria@mpaznotifica.cl", "inspectoria123")
        s.actualizarCanalPreferente(null, "AP1", CanalDespacho.SMS)

        assertEquals(listOf("login", "canal"), servidor.llamadas)
        assertTrue(local.llamadas.isEmpty())
        assertFalse(s.esLocal)
    }

    @Test
    fun elCambioDeModoEnCalienteSeAplicaEnLaSiguienteOperacion() = runTest {
        val preferencia = ModoEnMemoria(inicialLocal = false)
        val s = SelectorFuente(preferencia, local, servidor)

        s.sincronizar()
        preferencia.cambiar(true)
        s.sincronizar()
        preferencia.cambiar(false)
        s.sincronizar()

        assertEquals(listOf("sincronizar", "sincronizar"), servidor.llamadas)
        assertEquals(listOf("sincronizar"), local.llamadas)
    }

    @Test
    fun actualEntregaLaFuenteSegunLaPreferencia() {
        val preferencia = ModoEnMemoria(inicialLocal = true)
        val s = SelectorFuente(preferencia, local, servidor)

        assertSame(local, s.actual())
        preferencia.cambiar(false)
        assertSame(servidor, s.actual())
    }

    @Test
    fun losDatosObservadosCambianDeBaseAlCambiarElModo() = runTest {
        val preferencia = ModoEnMemoria(inicialLocal = true)
        val s = SelectorFuente(preferencia, local, servidor)

        assertEquals("local", s.usuarios.first().single().nombre)
        preferencia.cambiar(false)
        assertEquals("servidor", s.usuarios.first().single().nombre)
    }

    @Test
    fun laMensajeriaTambienDependeDelModo() {
        val preferencia = ModoEnMemoria(inicialLocal = true)
        val s = SelectorFuente(preferencia, local, servidor)

        assertSame(local.mensajeriaPropia, s.mensajeria())
        preferencia.cambiar(false)
        assertSame(servidor.mensajeriaPropia, s.mensajeria())
    }

    @Test
    fun alPrepararLosDatosInicialesSeSiembranAmbasBases() = runTest {
        selector(inicialLocal = true).asegurarDatosIniciales()

        assertEquals(listOf("asegurar"), local.llamadas)
        assertEquals(listOf("asegurar"), servidor.llamadas)
    }

    @Test
    fun restablecerSoloActuaSobreLaFuenteActiva() = runTest {
        val preferencia = ModoEnMemoria(inicialLocal = true)
        val s = SelectorFuente(preferencia, local, servidor)

        s.restablecer()
        preferencia.cambiar(false)
        s.restablecer()

        assertEquals(listOf("restablecer"), local.llamadas)
        assertEquals(listOf("restablecer"), servidor.llamadas)
    }

    @Test
    fun lasPreferenciasEnMemoriaPublicanElCambio() = runTest {
        val preferencia = ModoEnMemoria(inicialLocal = false)
        assertFalse(preferencia.modoLocal.value)

        preferencia.cambiar(true)

        assertTrue(preferencia.modoLocal.value)
        assertTrue(preferencia.esLocal())
    }

    @Test
    fun losDatosLocalesYLosDelServidorSonIndependientes() = runTest {
        // Lo creado en modo local no aparece al volver al modo servidor (no hay sincronización).
        val almacenLocal = AlmacenFalso()
        val almacenServidor = AlmacenFalso()
        val preferencia = ModoEnMemoria(inicialLocal = true)
        val s = SelectorFuente(preferencia, FuenteLocal(almacenLocal), FuenteLocalComoServidor(almacenServidor))

        val comunicacion = MockData.comunicados.first().copy(id = "NUEVO-LOCAL", fechaHora = "2026-11-20 09:00:00")
        val envios = MockData.envios.filter { it.comunicacionId == "C1" }.mapIndexed { i, e ->
            e.copy(id = "EV-L$i", comunicacionId = "NUEVO-LOCAL")
        }
        val resultado = s.registrarEnvio(comunicacion, "4° Básico A", envios)
        assertTrue(resultado is ApiResult.Success)
        assertTrue(s.comunicaciones.first().any { it.id == "NUEVO-LOCAL" })

        preferencia.cambiar(false)

        assertFalse(s.comunicaciones.first().any { it.id == "NUEVO-LOCAL" })
        assertEquals(MockData.comunicados.size, s.comunicaciones.first().size)
    }
}
