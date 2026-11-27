package com.mpaznotifica.app.data.repository

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * [PreferenciaModo] en SharedPreferences (el mismo archivo privado donde se guarda la dirección
 * del servidor). Mientras el usuario no elija, vale [inicialLocal]; desde que usa el interruptor,
 * vale lo que eligió (salvo que cambie el valor inicial de compilación, ver [ResolucionModo]).
 * [esLocal] lee la preferencia en cada llamada.
 */
class PreferenciasModo(contexto: Context, private val inicialLocal: Boolean) : PreferenciaModo {

    private val preferencias =
        contexto.applicationContext.getSharedPreferences("mpaz_servidor", Context.MODE_PRIVATE)

    private val estado = MutableStateFlow(esLocal())

    override val modoLocal: StateFlow<Boolean> = estado.asStateFlow()

    override fun esLocal(): Boolean =
        ResolucionModo.esLocal(
            guardado = if (preferencias.contains(CLAVE)) preferencias.getBoolean(CLAVE, inicialLocal) else null,
            inicialGuardado = if (preferencias.contains(CLAVE_INICIAL)) preferencias.getBoolean(CLAVE_INICIAL, inicialLocal) else null,
            inicialLocal = inicialLocal
        )

    override fun cambiar(local: Boolean) {
        preferencias.edit().putBoolean(CLAVE, local).putBoolean(CLAVE_INICIAL, inicialLocal).apply()
        estado.value = local
    }

    private companion object {
        const val CLAVE = "modo_local"
        const val CLAVE_INICIAL = "modo_local_inicial"
    }
}
