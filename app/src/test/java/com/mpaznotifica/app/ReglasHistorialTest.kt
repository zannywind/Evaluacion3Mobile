package com.mpaznotifica.app

import com.mpaznotifica.app.data.MockData
import com.mpaznotifica.app.domain.FiltrosHistorial
import com.mpaznotifica.app.domain.ReglasHistorial
import com.mpaznotifica.app.model.TipoComunicacion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** RF-09 y RF-10: bitácora y filtros por fecha, tipo y emisor. */
class ReglasHistorialTest {

    // En la base sintética: C1 (2026-09-01, U2, citación), C2 (2026-09-15, U1, salida), C3 (2026-10-05, U1, institucional).
    private val todos = MockData.comunicados

    private fun ids(filtros: FiltrosHistorial) = ReglasHistorial.aplicar(todos, filtros).map { it.id }

    @Test
    fun sinFiltrosVienenDelMasRecienteAlMasAntiguo() {
        assertEquals(listOf("C3", "C2", "C1"), ids(FiltrosHistorial()))
    }

    @Test
    fun filtraPorTipo() {
        assertEquals(listOf("C1"), ids(FiltrosHistorial(tipo = TipoComunicacion.CITACION)))
        assertTrue(ids(FiltrosHistorial(tipo = TipoComunicacion.EVENTO)).isEmpty())
    }

    @Test
    fun filtraPorEmisor() {
        assertEquals(listOf("C3", "C2"), ids(FiltrosHistorial(emisorId = "U1")))
        assertEquals(listOf("C1"), ids(FiltrosHistorial(emisorId = "U2")))
    }

    @Test
    fun filtraPorRangoDeFechasIncluyendoLosExtremos() {
        assertEquals(listOf("C3", "C2"), ids(FiltrosHistorial(desde = "2026-09-10")))
        assertEquals(listOf("C1"), ids(FiltrosHistorial(hasta = "2026-09-10")))
        assertEquals(listOf("C2"), ids(FiltrosHistorial(desde = "2026-09-15", hasta = "2026-09-15")))
        assertEquals(listOf("C2", "C1"), ids(FiltrosHistorial(desde = "2026-09-01", hasta = "2026-09-30")))
    }

    @Test
    fun combinaLosFiltrosConY() {
        assertEquals(listOf("C3"), ids(FiltrosHistorial(tipo = TipoComunicacion.INSTITUCIONAL, emisorId = "U1")))
        assertTrue(ids(FiltrosHistorial(tipo = TipoComunicacion.CITACION, emisorId = "U1")).isEmpty())
        assertEquals(
            listOf("C2"),
            ids(FiltrosHistorial(emisorId = "U1", desde = "2026-09-01", hasta = "2026-09-30"))
        )
    }

    @Test
    fun rangoInvertidoSeInformaYNoFiltraPorFecha() {
        val filtros = FiltrosHistorial(desde = "2026-10-01", hasta = "2026-09-01")
        assertEquals(ReglasHistorial.MSG_FECHAS_INVERTIDAS, ReglasHistorial.errorFechas(filtros.desde, filtros.hasta))
        assertEquals(3, ids(filtros).size)
    }

    @Test
    fun rangoValidoNoTieneError() {
        assertNull(ReglasHistorial.errorFechas(null, null))
        assertNull(ReglasHistorial.errorFechas("2026-09-01", null))
        assertNull(ReglasHistorial.errorFechas("2026-09-01", "2026-09-01"))
    }

    @Test
    fun fechaConFormatoInvalidoSeInforma() {
        assertEquals(ReglasHistorial.MSG_FORMATO_FECHA, ReglasHistorial.errorFechas("10/09/2026", null))
        assertEquals(ReglasHistorial.MSG_FORMATO_FECHA, ReglasHistorial.errorFechas(null, "2026-9-1"))
    }

    @Test
    fun soloSeAceptanFechasRealesDelCalendario() {
        assertTrue(ReglasHistorial.fechaValida("2026-09-01"))
        assertTrue(ReglasHistorial.fechaValida("2024-02-29"))
        assertFalse(ReglasHistorial.fechaValida("2026-02-29"))
        assertFalse(ReglasHistorial.fechaValida("2100-02-29"))
        assertFalse(ReglasHistorial.fechaValida("2026-02-31"))
        assertFalse(ReglasHistorial.fechaValida("2026-13-01"))
        assertFalse(ReglasHistorial.fechaValida("2026-04-31"))
        assertFalse(ReglasHistorial.fechaValida("2026-00-10"))
    }

    @Test
    fun cuentaLosFiltrosActivos() {
        assertEquals(0, FiltrosHistorial().cantidadActivos)
        assertEquals(3, FiltrosHistorial(tipo = TipoComunicacion.CITACION, emisorId = "U1", desde = "2026-09-01").cantidadActivos)
    }
}
