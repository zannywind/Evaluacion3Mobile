package com.mpaznotifica.app.data.remote

import okhttp3.Interceptor

/** En release no se registra el tráfico HTTP. */
object RegistroHttp {
    fun interceptores(): List<Interceptor> = emptyList()
}
