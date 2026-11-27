package com.mpaznotifica.app

import com.mpaznotifica.app.data.MockData
import com.mpaznotifica.app.domain.Fechas
import com.mpaznotifica.app.domain.ReglasComunicado
import com.mpaznotifica.app.model.AlcanceEnvio
import com.mpaznotifica.app.model.CanalDespacho
import com.mpaznotifica.app.model.EstadoEnvio
import com.mpaznotifica.app.domain.DatosReferencia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Utilidades de fecha (RF-11) e integridad de la base sintética (RNF-04). */
class FechasYDatosTest {

    @Test
    fun milisegundosUtcSeConvierteAFechaSinDesfaseDeZonaHoraria() {
        assertEquals("1970-01-01", Fechas.deMillisUtc(0L))
        val millis = Fechas.aMillisUtc("2026-09-15")
        assertNotNull(millis)
        assertEquals("2026-09-15", Fechas.deMillisUtc(millis!!))
    }

    @Test
    fun fechaInexistenteNoSeConvierte() {
        assertNull(Fechas.aMillisUtc("2026-02-31"))
        assertNull(Fechas.aMillisUtc("no-es-fecha"))
    }

    @Test
    fun formatoLegibleInvierteLaFecha() {
        assertEquals("15-09-2026 09:30", Fechas.legible("2026-09-15 09:30:00"))
        assertEquals("15-09-2026", Fechas.fechaLegible("2026-09-15"))
        assertEquals("texto libre", Fechas.legible("texto libre"))
    }

    @Test
    fun fechaHoraActualTieneElFormatoDeLaBitacora() {
        assertTrue(Regex("""\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}""").matches(Fechas.ahora()))
    }

    @Test
    fun cadaEstudianteTieneExactamenteUnApoderado() {
        MockData.estudiantes.forEach { estudiante ->
            val apoderados = MockData.apoderados.filter { estudiante.id in it.pupilosIds }
            assertEquals("Estudiante ${estudiante.id}", 1, apoderados.size)
        }
    }

    @Test
    fun unApoderadoPuedeTenerMasDeUnPupilo() {
        assertEquals(listOf("E1", "E4"), MockData.apoderados.first { it.id == "AP1" }.pupilosIds)
        val datos = DatosReferencia(MockData.cursos, MockData.talleres, MockData.estudiantes, MockData.apoderados)
        assertEquals(2, datos.nombresPupilos(MockData.apoderados.first { it.id == "AP1" }).size)
    }

    @Test
    fun cadaCursoTieneAlMenosUnApoderadoDestinatario() {
        val datos = DatosReferencia(MockData.cursos, MockData.talleres, MockData.estudiantes, MockData.apoderados)
        MockData.cursos.forEach { curso ->
            assertTrue(curso, ReglasComunicado.resolverApoderados(AlcanceEnvio.CURSO, curso, datos).isNotEmpty())
        }
    }

    @Test
    fun losIdentificadoresDeLaBaseSinteticaSonUnicos() {
        assertEquals(MockData.usuarios.size, MockData.usuarios.map { it.id }.toSet().size)
        assertEquals(MockData.usuarios.size, MockData.usuarios.map { it.usuario }.toSet().size)
        assertEquals(MockData.apoderados.size, MockData.apoderados.map { it.id }.toSet().size)
        assertEquals(MockData.estudiantes.size, MockData.estudiantes.map { it.id }.toSet().size)
        assertEquals(MockData.envios.size, MockData.envios.map { it.id }.toSet().size)
    }

    @Test
    fun elHistorialInicialEsCoherenteConSusEnvios() {
        val idsComunicados = MockData.comunicados.map { it.id }.toSet()
        val idsApoderados = MockData.apoderados.map { it.id }.toSet()
        MockData.envios.forEach {
            assertTrue(it.comunicacionId in idsComunicados)
            assertTrue(it.apoderadoId in idsApoderados)
        }
        val idsAutores = MockData.usuarios.map { it.id }.toSet()
        MockData.comunicados.forEach { assertTrue(it.autorId in idsAutores) }
    }

    @Test
    fun laAutorizacionSembradaVaSoloPorCorreo() {
        val firma = MockData.comunicados.first { it.requiereFirmaFisica }
        val envios = MockData.envios.filter { it.comunicacionId == firma.id }
        assertEquals(MockData.apoderados.size, envios.size)
        assertTrue(envios.all { it.canal == CanalDespacho.EMAIL })
        assertTrue(firma.cuerpo.endsWith(ReglasComunicado.AVISO_FIRMA_FISICA))
    }

    @Test
    fun laBaseIncluyeUnDespachoFallidoParaDemostrarlo() {
        assertTrue(MockData.envios.any { it.estado == EstadoEnvio.FALLIDO })
    }
}
