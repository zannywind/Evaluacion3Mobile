package com.mpaznotifica.app.domain

import com.mpaznotifica.app.model.ErroresLogin
import java.security.MessageDigest

/**
 * Reglas de autenticación (RF-01), sin dependencias de Android para poder probarlas en JVM.
 *
 * La clave no se guarda en texto plano: se compara el SHA-256 de (sal + clave). La sal es fija
 * y de DEMOSTRACIÓN; es suficiente para este MVP académico, pero un sistema real debe usar un
 * algoritmo de derivación lento (bcrypt/Argon2) con sal aleatoria por usuario y un backend.
 */
object Credenciales {

    const val LARGO_MIN_CLAVE = 6
    private const val SAL_DEMO = "mpaz-demo-2026|"
    private val regexCorreo = Regex("""^[\w.+-]+@[\w-]+(\.[\w-]+)+$""")

    fun hash(clave: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest((SAL_DEMO + clave).toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xFF) }

    fun coincide(clave: String, claveHash: String): Boolean = hash(clave) == claveHash

    /** Valida el formato de los campos antes de consultar la base (un mensaje por campo). */
    fun validar(usuario: String, clave: String): ErroresLogin = ErroresLogin(
        usuario = when {
            usuario.isBlank() -> "Ingresa tu correo institucional"
            !regexCorreo.matches(usuario.trim()) -> "El correo no tiene un formato válido"
            else -> null
        },
        clave = when {
            clave.isBlank() -> "Ingresa tu clave"
            clave.length < LARGO_MIN_CLAVE -> "La clave tiene al menos $LARGO_MIN_CLAVE caracteres"
            else -> null
        }
    )
}
