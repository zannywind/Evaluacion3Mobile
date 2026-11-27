package com.mpaznotifica.app.model

/** Errores de validación del formulario de redacción, uno por campo (null = campo válido). */
data class ErroresComunicado(
    val asunto: String? = null,
    val cuerpo: String? = null,
    val tipo: String? = null,
    val alcance: String? = null,
    val destino: String? = null
) {
    val hayErrores: Boolean
        get() = listOf(asunto, cuerpo, tipo, alcance, destino).any { it != null }
}

/** Errores de validación del inicio de sesión, uno por campo (null = campo válido). */
data class ErroresLogin(
    val usuario: String? = null,
    val clave: String? = null
) {
    val hayErrores: Boolean
        get() = usuario != null || clave != null
}

/** PDF elegido con el selector de archivos del sistema, aún sin copiar al almacenamiento de la app. */
data class AdjuntoPdf(
    val nombre: String,
    val tamanoBytes: Long,
    val uri: String
)

/** Datos que el formulario de redacción entrega al ViewModel para emitir un comunicado. */
data class SolicitudComunicado(
    val asunto: String,
    val cuerpo: String,
    val tipo: TipoComunicacion,
    val alcance: AlcanceEnvio,
    val destino: String?,
    val adjunto: AdjuntoPdf?,
    val requiereFirmaFisica: Boolean
)

/** Conteo de resultados de un envío. */
data class ResumenEnvio(
    val despachados: Int,
    val fallidos: Int,
    val requiereFirmaFisica: Boolean = false
) {
    val total: Int
        get() = despachados + fallidos

    companion object {
        fun de(envios: List<EnvioDestinatario>, requiereFirmaFisica: Boolean = false) = ResumenEnvio(
            despachados = envios.count { it.estado == EstadoEnvio.DESPACHADO },
            fallidos = envios.count { it.estado == EstadoEnvio.FALLIDO },
            requiereFirmaFisica = requiereFirmaFisica
        )
    }
}

/** Estado del envío en curso, para el progreso y la alerta visual de confirmación (RNF-01). */
sealed interface EstadoEnvioUi {
    data object Inactivo : EstadoEnvioUi
    data class Enviando(val procesados: Int, val total: Int) : EstadoEnvioUi
    data class Listo(val resumen: ResumenEnvio) : EstadoEnvioUi
    /** [puedeReintentar]: el comunicado ya se despachó y solo falta registrarlo en el servidor. */
    data class Error(val mensaje: String, val puedeReintentar: Boolean = false) : EstadoEnvioUi
}
