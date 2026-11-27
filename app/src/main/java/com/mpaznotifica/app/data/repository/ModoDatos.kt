package com.mpaznotifica.app.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Preferencia persistente "modo local / modo servidor".
 *
 *  - Modo local: la app opera solo con su base Room (sin red), con las mismas reglas de negocio.
 *    Es el respaldo para poder demostrar todo el día de la defensa si el servicio no está disponible.
 *  - Modo servidor: la ruta normal (API + caché Room). Es el que se demuestra en la defensa.
 *
 * El valor se consulta con [esLocal] EN CADA OPERACIÓN (no solo al arrancar), de modo que el
 * cambio es inmediato. [modoLocal] avisa a quien observa (datos en pantalla, franja de modo).
 */
interface PreferenciaModo {
    val modoLocal: StateFlow<Boolean>

    fun esLocal(): Boolean

    fun cambiar(local: Boolean)
}

/** Implementación en memoria (sin Android): la usan las pruebas JVM. */
class ModoEnMemoria(inicialLocal: Boolean) : PreferenciaModo {
    private val estado = MutableStateFlow(inicialLocal)

    override val modoLocal: StateFlow<Boolean> = estado.asStateFlow()

    override fun esLocal(): Boolean = estado.value

    override fun cambiar(local: Boolean) {
        estado.value = local
    }
}

/** Regla de qué modo vale al leer la preferencia guardada (pura, probada en JVM). */
object ResolucionModo {
    /**
     * [guardado]: lo que el usuario eligió con el interruptor (null si nunca lo usó).
     * [inicialGuardado]: el valor inicial de compilación que regía cuando eligió.
     * [inicialLocal]: el valor inicial de esta compilación.
     *
     * Si nunca eligió, vale el valor inicial. Si eligió pero desde entonces cambió el valor inicial
     * de compilación (se editó gradle.properties y se recompiló), la elección antigua ya no vale y
     * rige el nuevo valor inicial. En cualquier otro caso manda la elección del usuario.
     */
    fun esLocal(guardado: Boolean?, inicialGuardado: Boolean?, inicialLocal: Boolean): Boolean = when {
        guardado == null -> inicialLocal
        inicialGuardado != null && inicialGuardado != inicialLocal -> inicialLocal
        else -> guardado
    }
}
