package com.mpaznotifica.app.model

/**
 * Perfiles emisores del caso. El apoderado es solo destinatario: no opera la app, por eso no
 * tiene perfil ni inicio de sesión.
 */
enum class Perfil(val etiqueta: String) {
    INSPECTORIA_GENERAL("Inspectoría General"),
    PROFESOR_JEFE("Profesor/a Jefe")
}
