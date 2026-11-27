package com.mpaznotifica.app.modo

import com.mpaznotifica.app.data.repository.ResolucionModo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Qué modo vale al leer la preferencia: valor inicial, elección del usuario y cambio del valor inicial. */
class ResolucionModoTest {

    @Test
    fun sinEleccionDelUsuarioValeElValorInicial() {
        assertTrue(ResolucionModo.esLocal(guardado = null, inicialGuardado = null, inicialLocal = true))
        assertFalse(ResolucionModo.esLocal(guardado = null, inicialGuardado = null, inicialLocal = false))
    }

    @Test
    fun laEleccionDelUsuarioMandaSobreElValorInicial() {
        // La app parte en local, pero el usuario eligió servidor.
        assertFalse(ResolucionModo.esLocal(guardado = false, inicialGuardado = true, inicialLocal = true))
        // La app parte en servidor, pero el usuario eligió local.
        assertTrue(ResolucionModo.esLocal(guardado = true, inicialGuardado = false, inicialLocal = false))
    }

    @Test
    fun siCambiaElValorInicialDeCompilacionLaEleccionAntiguaYaNoVale() {
        // Eligió local cuando la app partía en local; luego se recompiló para partir en servidor.
        assertFalse(ResolucionModo.esLocal(guardado = true, inicialGuardado = true, inicialLocal = false))
        // Eligió servidor cuando la app partía en servidor; luego se recompiló para partir en local.
        assertTrue(ResolucionModo.esLocal(guardado = false, inicialGuardado = false, inicialLocal = true))
    }
}
