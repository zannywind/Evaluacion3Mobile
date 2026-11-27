package com.mpaznotifica.app.data.remote

import android.content.Context

/** Almacén de la dirección elegida por el usuario (separado de Android para poder probarlo). */
interface AlmacenUrl {
    fun leer(): String?
    fun guardar(url: String)
    fun borrar()
}

/** SharedPreferences privadas de la app: la dirección persiste entre aperturas. Ya implementada. */
class PreferenciasUrl(contexto: Context) : AlmacenUrl {
    private val preferencias =
        contexto.applicationContext.getSharedPreferences("mpaz_servidor", Context.MODE_PRIVATE)

    override fun leer(): String? = preferencias.getString(CLAVE, null)

    override fun guardar(url: String) {
        preferencias.edit().putString(CLAVE, url).apply()
    }

    override fun borrar() {
        preferencias.edit().remove(CLAVE).apply()
    }

    private companion object {
        const val CLAVE = "url_base"
    }
}

/**
 * EXPERIENCIA 3 - Parte 3 (TODO E3-9b): dirección activa del servidor.
 *
 * Prioridad: la que guardó el usuario en el diálogo "Servidor"; si no hay (o dejó de ser válida),
 * la de compilación ([urlCompilacion] = BuildConfig.API_BASE_URL, desde gradle.properties); si
 * tampoco es válida, [UrlServidor.DEFECTO].
 */
class ConfiguracionServidor(
    private val almacen: AlmacenUrl,
    private val urlCompilacion: String
) {
    /**
     * TODO(E3-9b): dirección en uso, siempre válida y terminada en "/".
     * Criterio: guardada válida > compilación válida > [UrlServidor.DEFECTO]; el resultado pasa
     * por [UrlServidor.normalizar].
     */
    fun urlBase(): String =
        TODO("E3-9b: elegir entre almacen.leer(), urlCompilacion y UrlServidor.DEFECTO usando UrlServidor.esValida")

    /**
     * TODO(E3-9b): guarda la dirección si es válida. Devuelve el mensaje de error de
     * [UrlServidor.validar] o null si se guardó. Lo que se guarda va normalizado (con "/" final).
     * Si hay error NO se escribe nada en el almacén.
     */
    fun guardar(texto: String): String? =
        TODO("E3-9b: validar, normalizar y almacen.guardar(...)")

    /** TODO(E3-9b): vuelve a la dirección de compilación (borra la guardada). */
    fun restaurar(): Unit =
        TODO("E3-9b: almacen.borrar()")

    /** Dirección de compilación normalizada (ya implementada). */
    val valorCompilacion: String get() = UrlServidor.normalizar(urlCompilacion)
}
