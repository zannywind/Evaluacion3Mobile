package com.mpaznotifica.app.data.remote

import java.net.URI
import java.net.URISyntaxException

/**
 * EXPERIENCIA 3 - Parte 3 (TODO E3-9a): validación de la dirección del servidor (diálogo "Servidor"
 * de la pantalla de ingreso). Kotlin puro: se prueba en JVM sin emulador.
 *
 * Reglas: esquema http o https, host presente, puerto 1..65535, sin usuario/clave, consulta ni
 * fragmento. https se acepta para cualquier host; http (sin cifrar) solo para desarrollo local:
 * 10.0.2.2 (emulador), localhost, 127.x y redes privadas (10.x, 172.16-31.x, 192.168.x, 169.254.x).
 */
object UrlServidor {

    const val DEFECTO = "http://10.0.2.2:3000/"

    const val MSG_VACIA = "Ingresa la dirección del servidor"
    const val MSG_FORMATO = "La dirección no es válida. Ejemplo: http://192.168.1.10:3000/"
    const val MSG_ESQUEMA = "La dirección debe comenzar con http:// o https://"
    const val MSG_HOST = "Falta el nombre o la IP del servidor"
    const val MSG_PUERTO = "El puerto debe estar entre 1 y 65535"
    const val MSG_EXTRAS = "La dirección no debe incluir usuario, parámetros ni #"
    const val MSG_CLEARTEXT =
        "Una dirección pública debe usar https://. El http:// solo se permite hacia 10.0.2.2, localhost o una IP de red local"

    /** Texto limpio y terminado en "/" (lo exige Retrofit). No valida: usar [validar] antes. */
    fun normalizar(texto: String): String {
        val limpio = texto.trim()
        return if (limpio.endsWith("/")) limpio else "$limpio/"
    }

    /**
     * TODO(E3-9a): mensaje de error para mostrar bajo el campo, o null si la dirección sirve.
     * Archivo: este. Criterio de aceptación (en este orden; devuelve el primer error que encuentre):
     *  1. Vacía o en blanco -> [MSG_VACIA].
     *  2. Con espacios en medio -> [MSG_FORMATO].
     *  3. Sin "://" -> [MSG_ESQUEMA].
     *  4. `java.net.URI` no la puede interpretar ([URISyntaxException]) -> [MSG_FORMATO].
     *  5. Esquema distinto de http/https -> [MSG_ESQUEMA].
     *  6. Sin host -> [MSG_HOST] (si tampoco hay `authority`) o [MSG_FORMATO].
     *  7. Con usuario/clave, consulta (?) o fragmento (#) -> [MSG_EXTRAS].
     *  8. Puerto explícito fuera de 1..65535 -> [MSG_PUERTO].
     *  9. http hacia un host para el que [permiteCleartext] es false -> [MSG_CLEARTEXT].
     * 10. En cualquier otro caso -> null. Mayúsculas en el esquema se aceptan ("HTTPS://...").
     * Casos que debe resolver: "http://10.0.2.2:3000/" (ok), "https://miservidor.example.com" (ok),
     * "http://miservidor.example.com" ([MSG_CLEARTEXT]), "ftp://x" ([MSG_ESQUEMA]),
     * "http://localhost:99999" ([MSG_PUERTO]), "http://a b" ([MSG_FORMATO]).
     */
    fun validar(texto: String): String? =
        TODO("E3-9a: aplicar las 10 reglas del comentario con java.net.URI")

    fun esValida(texto: String): Boolean = validar(texto) == null

    /** true si se puede usar http sin cifrar hacia [host] (desarrollo local). Ya implementada. */
    fun permiteCleartext(host: String): Boolean {
        val h = host.trim().lowercase().removePrefix("[").removeSuffix("]")
        if (h == "localhost" || h == "10.0.2.2" || h == "::1") return true
        val octetos = h.split(".")
        if (octetos.size != 4) return false
        val n = octetos.map { it.toIntOrNull() ?: return false }
        if (n.any { it !in 0..255 }) return false
        return when {
            n[0] == 127 -> true
            n[0] == 10 -> true
            n[0] == 172 && n[1] in 16..31 -> true
            n[0] == 192 && n[1] == 168 -> true
            n[0] == 169 && n[1] == 254 -> true
            else -> false
        }
    }
}
