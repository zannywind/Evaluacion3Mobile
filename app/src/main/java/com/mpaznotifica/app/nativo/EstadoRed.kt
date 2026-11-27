package com.mpaznotifica.app.nativo

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/** Consulta si el dispositivo tiene alguna red con acceso a internet (permiso ACCESS_NETWORK_STATE). */
object EstadoRed {
    fun disponible(contexto: Context): Boolean {
        val administrador =
            contexto.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val red = administrador.activeNetwork ?: return false
        val capacidades = administrador.getNetworkCapabilities(red) ?: return false
        return capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
