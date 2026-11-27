package com.mpaznotifica.app.model

enum class TipoComunicacion(val etiqueta: String) {
    INSTITUCIONAL("Institucional"),
    PEDAGOGICA("Pedagógica"),
    DISCIPLINARIA("Disciplinaria"),
    CITACION("Citación"),
    EVENTO("Evento"),
    TALLER("Taller"),
    SALIDA_PEDAGOGICA("Salida pedagógica")
}

enum class AlcanceEnvio(val etiqueta: String) {
    APODERADO_INDIVIDUAL("Apoderado individual"),
    CURSO("Curso"),
    TALLER("Taller"),
    COLEGIO_COMPLETO("Colegio completo")
}

enum class EstadoEnvio(val etiqueta: String) {
    DESPACHADO("Despachado"),
    FALLIDO("Fallido")
}

/**
 * Comunicado emitido (entidad "Comunicación" del caso).
 *
 * RN-02: una vez enviado queda registrado de forma INMUTABLE (no se edita ni se elimina).
 * [requiereFirmaFisica] marca una autorización con firma legal (RF-12): la app solo informa por
 * correo y alerta que corresponde entrega física para firma manuscrita; no captura firmas.
 * [fechaHora] se guarda como texto "AAAA-MM-DD HH:mm:ss", ordenable y comparable como texto.
 */
data class Comunicacion(
    val id: String,
    val asunto: String,
    val cuerpo: String,
    val tipo: TipoComunicacion,
    val alcance: AlcanceEnvio,
    val destinoReferencia: String? = null,
    val adjuntoPdfNombre: String? = null,
    val requiereFirmaFisica: Boolean = false,
    val autorId: String,
    val autorNombre: String,
    val fechaHora: String
)

/** Resultado del despacho simulado a UN destinatario (RF-08), con el canal empleado (RF-11). */
data class EnvioDestinatario(
    val id: String,
    val comunicacionId: String,
    val apoderadoId: String,
    val canal: CanalDespacho,
    val estado: EstadoEnvio,
    val detalle: String = ""
)

/** Apoderado destinatario junto con el canal por el que se le despachará. */
data class Destinatario(
    val apoderado: Apoderado,
    val canal: CanalDespacho
)
