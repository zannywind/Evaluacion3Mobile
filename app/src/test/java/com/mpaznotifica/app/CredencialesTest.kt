package com.mpaznotifica.app

import com.mpaznotifica.app.data.MockData
import com.mpaznotifica.app.domain.Credenciales
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** RF-01: inicio de sesión (validación por campo y verificación por hash). */
class CredencialesTest {

    @Test
    fun hashEsSha256HexadecimalDe64Caracteres() {
        val hash = Credenciales.hash("cualquierClave")
        assertEquals(64, hash.length)
        assertTrue(hash.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun hashEsDeterministaYDependeDeLaClave() {
        assertEquals(Credenciales.hash("clave123"), Credenciales.hash("clave123"))
        assertNotEquals(Credenciales.hash("clave123"), Credenciales.hash("clave124"))
    }

    @Test
    fun noSeGuardaLaClaveEnTextoPlano() {
        MockData.usuarios.forEach { usuario ->
            assertFalse(usuario.claveHash.contains("123"))
        }
    }

    @Test
    fun usuariosDeDemostracionCoincidenConSuClave() {
        val inspectoria = MockData.usuarios.first { it.id == "U1" }
        val profesor = MockData.usuarios.first { it.id == "U2" }
        assertTrue(Credenciales.coincide("inspectoria123", inspectoria.claveHash))
        assertTrue(Credenciales.coincide("profesor123", profesor.claveHash))
        assertFalse(Credenciales.coincide("profesor123", inspectoria.claveHash))
        assertFalse(Credenciales.coincide("", profesor.claveHash))
    }

    @Test
    fun camposVaciosMarcanErrorEnCadaCampo() {
        val errores = Credenciales.validar("", "")
        assertNotNull(errores.usuario)
        assertNotNull(errores.clave)
        assertTrue(errores.hayErrores)
    }

    @Test
    fun correoSinFormatoValidoMarcaErrorSoloEnElCorreo() {
        val errores = Credenciales.validar("correo-sin-arroba", "profesor123")
        assertNotNull(errores.usuario)
        assertNull(errores.clave)
    }

    @Test
    fun claveCortaMarcaErrorSoloEnLaClave() {
        val errores = Credenciales.validar("inspectoria@mpaznotifica.cl", "12345")
        assertNull(errores.usuario)
        assertNotNull(errores.clave)
    }

    @Test
    fun camposValidosNoTienenErrores() {
        assertFalse(Credenciales.validar("profesor.4basicoA@mpaznotifica.cl", "profesor123").hayErrores)
    }
}
