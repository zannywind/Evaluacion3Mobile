package com.mpaznotifica.app.data.remote

/** Token de la sesión actual (en memoria: se pierde al cerrar la app, por diseño). */
class SesionApi {
    @Volatile
    var token: String? = null
        private set

    fun iniciar(token: String) {
        this.token = token
    }

    fun cerrar() {
        token = null
    }

    val activa: Boolean get() = token != null
}
