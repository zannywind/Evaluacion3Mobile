package com.mpaznotifica.app

import com.mpaznotifica.app.data.MockData
import com.mpaznotifica.app.domain.DatosReferencia
import com.mpaznotifica.app.domain.ReglasComunicado
import com.mpaznotifica.app.model.AlcanceEnvio
import com.mpaznotifica.app.model.Apoderado
import com.mpaznotifica.app.model.CanalDespacho
import com.mpaznotifica.app.model.Estudiante
import com.mpaznotifica.app.model.Taller
import com.mpaznotifica.app.model.TipoComunicacion
import com.mpaznotifica.app.model.Usuario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** RF-02 a RF-06, RF-12, RF-13 y RN-01: reglas de emisión por perfil. */
class ReglasComunicadoTest {

    private val inspectoria = MockData.usuarios.first { it.id == "U1" }
    private val profesor4A = MockData.usuarios.first { it.id == "U2" }
    private val profesor2A = MockData.usuarios.first { it.id == "U3" }
    private val datos = DatosReferencia(MockData.cursos, MockData.talleres, MockData.estudiantes, MockData.apoderados)

    private fun validar(
        usuario: Usuario?,
        tipo: TipoComunicacion? = TipoComunicacion.PEDAGOGICA,
        alcance: AlcanceEnvio? = AlcanceEnvio.CURSO,
        destino: String? = "4° Básico A",
        asunto: String = "Aviso de aula",
        cuerpo: String = "Mañana se retoma la actividad en la sala."
    ) = ReglasComunicado.validar(asunto, cuerpo, tipo, alcance, destino, usuario, datos)

    // ------------------------------------------------------------- RF-02 / RF-05 / RF-06

    @Test
    fun inspectoriaEmiteALosCuatroAlcancesYTodosLosTipos() {
        assertEquals(AlcanceEnvio.entries.toSet(), ReglasComunicado.alcancesPermitidos(inspectoria).toSet())
        assertEquals(TipoComunicacion.entries.toSet(), ReglasComunicado.tiposPermitidos(inspectoria).toSet())
    }

    @Test
    fun profesorSoloEmiteASuCursoOAApoderadosDeSuCurso() {
        val alcances = ReglasComunicado.alcancesPermitidos(profesor4A)
        assertEquals(setOf(AlcanceEnvio.CURSO, AlcanceEnvio.APODERADO_INDIVIDUAL), alcances.toSet())
        assertFalse(AlcanceEnvio.COLEGIO_COMPLETO in alcances)
        assertFalse(AlcanceEnvio.TALLER in alcances)
    }

    @Test
    fun profesorNoEmiteComunicadosInstitucionalesNiDeEvento() {
        val tipos = ReglasComunicado.tiposPermitidos(profesor4A)
        assertFalse(TipoComunicacion.INSTITUCIONAL in tipos)
        assertFalse(TipoComunicacion.EVENTO in tipos)
        assertTrue(TipoComunicacion.PEDAGOGICA in tipos)
        assertTrue(TipoComunicacion.CITACION in tipos)
    }

    @Test
    fun sinSesionNoHayOpcionesDeEmision() {
        assertTrue(ReglasComunicado.alcancesPermitidos(null).isEmpty())
        assertTrue(ReglasComunicado.tiposPermitidos(null).isEmpty())
        assertTrue(ReglasComunicado.destinosPara(AlcanceEnvio.CURSO, null, datos).isEmpty())
    }

    // ------------------------------------------------------------- RF-04: destinos

    @Test
    fun inspectoriaVeTodosLosCursosTalleresYApoderados() {
        assertEquals(MockData.cursos, ReglasComunicado.destinosPara(AlcanceEnvio.CURSO, inspectoria, datos))
        assertEquals(
            MockData.talleres.map { it.nombre },
            ReglasComunicado.destinosPara(AlcanceEnvio.TALLER, inspectoria, datos)
        )
        assertEquals(
            MockData.apoderados.map { it.nombreCompleto },
            ReglasComunicado.destinosPara(AlcanceEnvio.APODERADO_INDIVIDUAL, inspectoria, datos)
        )
        assertTrue(ReglasComunicado.destinosPara(AlcanceEnvio.COLEGIO_COMPLETO, inspectoria, datos).isEmpty())
    }

    @Test
    fun profesorVeSoloSuCursoYLosApoderadosDeEseCurso() {
        assertEquals(listOf("4° Básico A"), ReglasComunicado.destinosPara(AlcanceEnvio.CURSO, profesor4A, datos))
        assertEquals(
            listOf("Apoderado Ficticio 1", "Apoderado Ficticio 2", "Apoderado Ficticio 4"),
            ReglasComunicado.destinosPara(AlcanceEnvio.APODERADO_INDIVIDUAL, profesor4A, datos)
        )
        assertEquals(listOf("2° Básico A"), ReglasComunicado.destinosPara(AlcanceEnvio.CURSO, profesor2A, datos))
        assertEquals(
            listOf("Apoderado Ficticio 3"),
            ReglasComunicado.destinosPara(AlcanceEnvio.APODERADO_INDIVIDUAL, profesor2A, datos)
        )
        assertTrue(ReglasComunicado.destinosPara(AlcanceEnvio.TALLER, profesor4A, datos).isEmpty())
        assertTrue(ReglasComunicado.destinosPara(AlcanceEnvio.COLEGIO_COMPLETO, profesor4A, datos).isEmpty())
    }

    @Test
    fun alcanceCursoResuelveLosApoderadosDeLosPupilosDeEseCurso() {
        val ids = ReglasComunicado.resolverApoderados(AlcanceEnvio.CURSO, "4° Básico A", datos).map { it.id }
        assertEquals(listOf("AP1", "AP2", "AP4"), ids)
    }

    @Test
    fun alcanceTallerResuelveLosApoderadosDeLosInscritos() {
        assertEquals(
            listOf("AP1"),
            ReglasComunicado.resolverApoderados(AlcanceEnvio.TALLER, "Taller de Robótica", datos).map { it.id }
        )
        assertEquals(
            listOf("AP1", "AP3", "AP4"),
            ReglasComunicado.resolverApoderados(AlcanceEnvio.TALLER, "Taller de Fútbol", datos).map { it.id }
        )
        assertTrue(ReglasComunicado.resolverApoderados(AlcanceEnvio.TALLER, "Taller inexistente", datos).isEmpty())
    }

    @Test
    fun alcanceIndividualResuelveUnSoloApoderado() {
        val ids = ReglasComunicado.resolverApoderados(AlcanceEnvio.APODERADO_INDIVIDUAL, "Apoderado Ficticio 3", datos)
            .map { it.id }
        assertEquals(listOf("AP3"), ids)
    }

    @Test
    fun alcanceColegioCompletoResuelveATodosLosApoderados() {
        assertEquals(
            MockData.apoderados.size,
            ReglasComunicado.resolverApoderados(AlcanceEnvio.COLEGIO_COMPLETO, null, datos).size
        )
    }

    @Test
    fun apoderadoConDosPupilosEnElMismoCursoRecibeUnSoloComunicado() {
        val estudiantes = listOf(
            Estudiante("E1", "Estudiante Ficticio 1", "4° Básico A"),
            Estudiante("E2", "Estudiante Ficticio 2", "4° Básico A", listOf("T1"))
        )
        val apoderado = Apoderado("AP1", "Apoderado Ficticio 1", "a@ejemplo.cl", "+56900000001", CanalDespacho.EMAIL, listOf("E1", "E2"))
        val d = DatosReferencia(listOf("4° Básico A"), listOf(Taller("T1", "Taller de Robótica")), estudiantes, listOf(apoderado))
        assertEquals(1, ReglasComunicado.resolverApoderados(AlcanceEnvio.CURSO, "4° Básico A", d).size)
        assertEquals(1, ReglasComunicado.resolverApoderados(AlcanceEnvio.TALLER, "Taller de Robótica", d).size)
    }

    // ------------------------------------------------------------- RN-01 y validación RF-03

    @Test
    fun rn01ProfesorNoPuedeEmitirAColegioCompleto() {
        val errores = validar(profesor4A, alcance = AlcanceEnvio.COLEGIO_COMPLETO, destino = null)
        assertEquals(ReglasComunicado.MSG_SOLO_CURSO, errores.alcance)
    }

    @Test
    fun rn01ProfesorNoPuedeEmitirATalleres() {
        val errores = validar(profesor4A, alcance = AlcanceEnvio.TALLER, destino = "Taller de Fútbol")
        assertEquals(ReglasComunicado.MSG_SOLO_CURSO, errores.alcance)
    }

    @Test
    fun rn01ProfesorNoPuedeEmitirAUnCursoDistintoDelPropio() {
        val errores = validar(profesor4A, destino = "2° Básico A")
        assertEquals(ReglasComunicado.MSG_SOLO_CURSO, errores.destino)
        assertNull(errores.alcance)
    }

    @Test
    fun rn01ProfesorNoPuedeEmitirAUnApoderadoDeOtroCurso() {
        val errores = validar(
            profesor4A,
            alcance = AlcanceEnvio.APODERADO_INDIVIDUAL,
            destino = "Apoderado Ficticio 3"
        )
        assertEquals(ReglasComunicado.MSG_SOLO_CURSO, errores.destino)
    }

    @Test
    fun profesorEmiteSinErroresASuCursoYAUnApoderadoDeSuCurso() {
        assertFalse(validar(profesor4A).hayErrores)
        assertFalse(
            validar(
                profesor4A,
                tipo = TipoComunicacion.CITACION,
                alcance = AlcanceEnvio.APODERADO_INDIVIDUAL,
                destino = "Apoderado Ficticio 2"
            ).hayErrores
        )
    }

    @Test
    fun profesorNoPuedeEmitirTipoInstitucional() {
        val errores = validar(profesor4A, tipo = TipoComunicacion.INSTITUCIONAL)
        assertEquals(ReglasComunicado.MSG_TIPO_NO_DISPONIBLE, errores.tipo)
    }

    @Test
    fun inspectoriaEmiteSinErroresACualquierAlcance() {
        AlcanceEnvio.entries.forEach { alcance ->
            val destino = ReglasComunicado.destinosPara(alcance, inspectoria, datos).firstOrNull()
            val errores = validar(inspectoria, tipo = TipoComunicacion.INSTITUCIONAL, alcance = alcance, destino = destino)
            assertFalse("Alcance $alcance: $errores", errores.hayErrores)
        }
    }

    @Test
    fun formularioVacioMarcaErrorEnCadaCampoObligatorio() {
        val errores = ReglasComunicado.validar("", "", null, null, null, inspectoria, datos)
        assertNotNull(errores.asunto)
        assertNotNull(errores.cuerpo)
        assertNotNull(errores.tipo)
        assertNotNull(errores.alcance)
        assertNull(errores.destino)
    }

    @Test
    fun alcanceDistintoDeColegioExigeDestino() {
        assertNotNull(validar(inspectoria, destino = null).destino)
        assertNotNull(validar(inspectoria, destino = "").destino)
    }

    @Test
    fun asuntoYCuerpoRespetanLosLargosPermitidos() {
        assertNotNull(validar(inspectoria, asunto = "ab").asunto)
        assertNotNull(validar(inspectoria, asunto = "a".repeat(ReglasComunicado.MAX_ASUNTO + 1)).asunto)
        assertNull(validar(inspectoria, asunto = "a".repeat(ReglasComunicado.MAX_ASUNTO)).asunto)
        assertNotNull(validar(inspectoria, cuerpo = "corto").cuerpo)
        assertNotNull(validar(inspectoria, cuerpo = "c".repeat(ReglasComunicado.MAX_CUERPO + 1)).cuerpo)
        assertNull(validar(inspectoria, cuerpo = "c".repeat(ReglasComunicado.MIN_CUERPO)).cuerpo)
    }

    @Test
    fun sinSesionElComunicadoNoSeValida() {
        assertEquals(ReglasComunicado.MSG_SIN_SESION, validar(null).alcance)
    }

    @Test
    fun destinoSinApoderadosDaError() {
        val sinInscritos = datos.copy(talleres = datos.talleres + Taller("T9", "Taller sin inscritos"))
        val errores = ReglasComunicado.validar(
            "Aviso de taller", "No hay estudiantes inscritos todavía.",
            TipoComunicacion.TALLER, AlcanceEnvio.TALLER, "Taller sin inscritos", inspectoria, sinInscritos
        )
        assertEquals(ReglasComunicado.MSG_SIN_DESTINATARIOS, errores.destino)
    }

    // ------------------------------------------------------------- adjunto PDF

    @Test
    fun adjuntoDebeSerPdfYNoSuperarElMaximo() {
        assertNull(ReglasComunicado.errorAdjunto("application/pdf", 1024L))
        assertNull(ReglasComunicado.errorAdjunto("application/pdf", ReglasComunicado.MAX_ADJUNTO_BYTES))
        assertNotNull(ReglasComunicado.errorAdjunto("image/png", 1024L))
        assertNotNull(ReglasComunicado.errorAdjunto(null, 1024L))
        assertNotNull(ReglasComunicado.errorAdjunto("application/pdf", ReglasComunicado.MAX_ADJUNTO_BYTES + 1))
    }

    // ------------------------------------------------------------- RF-12: firma física

    @Test
    fun autorizacionConFirmaFisicaSeInformaSiempreCorreo() {
        val whatsapp = MockData.apoderados.first { it.id == "AP2" }
        assertEquals(CanalDespacho.WHATSAPP, ReglasComunicado.canalDe(whatsapp, requiereFirmaFisica = false))
        assertEquals(CanalDespacho.EMAIL, ReglasComunicado.canalDe(whatsapp, requiereFirmaFisica = true))
    }

    @Test
    fun autorizacionConFirmaFisicaAgregaElAvisoDeEntregaEnPapel() {
        val conAviso = ReglasComunicado.cuerpoFinal("  Texto del comunicado  ", requiereFirmaFisica = true)
        assertTrue(conAviso.startsWith("Texto del comunicado"))
        assertTrue(conAviso.endsWith(ReglasComunicado.AVISO_FIRMA_FISICA))
        assertEquals("Texto del comunicado", ReglasComunicado.cuerpoFinal("  Texto del comunicado  ", false))
    }

    // ------------------------------------------------------------- RF-13: canal preferente

    @Test
    fun soloInspectoriaConfiguraElCanalPreferente() {
        assertTrue(ReglasComunicado.puedeConfigurarCanal(inspectoria))
        assertFalse(ReglasComunicado.puedeConfigurarCanal(profesor4A))
        assertFalse(ReglasComunicado.puedeConfigurarCanal(null))
    }

    @Test
    fun cadaPerfilVeLosApoderadosQueLeCorresponden() {
        assertEquals(MockData.apoderados.size, ReglasComunicado.apoderadosVisibles(inspectoria, datos).size)
        assertEquals(
            listOf("AP1", "AP2", "AP4"),
            ReglasComunicado.apoderadosVisibles(profesor4A, datos).map { it.id }
        )
        assertTrue(ReglasComunicado.apoderadosVisibles(null, datos).isEmpty())
    }
}
