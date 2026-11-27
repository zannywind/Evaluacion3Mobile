package com.mpaznotifica.app.model

/**
 * Usuario emisor que inicia sesión. [cursoAsignado] solo aplica al Profesor/a Jefe (su curso de
 * jefatura). La clave nunca se guarda en texto plano: [claveHash] es el SHA-256 con sal de demo
 * (ver domain/Credenciales.kt).
 */
data class Usuario(
    val id: String,
    val nombre: String,
    val usuario: String,
    val claveHash: String,
    val perfil: Perfil,
    val cursoAsignado: String? = null
)
