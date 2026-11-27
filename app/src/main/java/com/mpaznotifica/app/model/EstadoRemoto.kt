package com.mpaznotifica.app.model

/** Estado de la sincronización con el servidor, para el aviso de conexión de las pantallas. */
sealed interface EstadoDatos {
    data object Inactivo : EstadoDatos
    data object Cargando : EstadoDatos

    /** Los datos mostrados acaban de leerse del servidor. [hora] es "dd-MM-aaaa HH:mm". */
    data class Actualizado(val hora: String) : EstadoDatos

    /** No hubo conexión: se muestran los últimos datos guardados en el dispositivo. */
    data class SinConexion(val mensaje: String) : EstadoDatos

    /** El servidor respondió con un error. */
    data class Error(val mensaje: String) : EstadoDatos
}

/** Resultado de "Probar conexión" en la pantalla Servidor. */
sealed interface PruebaServidor {
    data object Inactiva : PruebaServidor
    data object Probando : PruebaServidor
    data class Correcta(val mensaje: String) : PruebaServidor
    data class Fallida(val mensaje: String) : PruebaServidor
}
