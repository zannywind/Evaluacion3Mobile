package com.mpaznotifica.app.model

enum class CanalDespacho(val etiqueta: String) {
    EMAIL("Correo electrónico"),
    SMS("SMS"),
    WHATSAPP("WhatsApp")
}

/**
 * Apoderado (destinatario final, no opera la app). Un apoderado puede tener más de un pupilo
 * (supuesto pendiente de confirmar con el CITT) y su canal preferente es configurable (RF-13).
 */
data class Apoderado(
    val id: String,
    val nombreCompleto: String,
    val correo: String,
    val telefono: String,
    val canalPreferente: CanalDespacho = CanalDespacho.EMAIL,
    val pupilosIds: List<String> = emptyList()
)
