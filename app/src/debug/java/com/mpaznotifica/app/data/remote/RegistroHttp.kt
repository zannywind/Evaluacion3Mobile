package com.mpaznotifica.app.data.remote

import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor

/** Registro HTTP solo en builds debug (Logcat). Oculta la cabecera Authorization. */
object RegistroHttp {
    fun interceptores(): List<Interceptor> = listOf(
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
            redactHeader("Authorization")
        }
    )
}
