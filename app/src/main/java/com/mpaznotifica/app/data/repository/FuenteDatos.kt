package com.mpaznotifica.app.data.repository

import com.mpaznotifica.app.data.remote.ApiResult
import com.mpaznotifica.app.data.remote.ComunicacionRegistrada
import com.mpaznotifica.app.data.remote.MensajeriaApi
import com.mpaznotifica.app.data.remote.SesionRemota
import com.mpaznotifica.app.model.Apoderado
import com.mpaznotifica.app.model.CanalDespacho
import com.mpaznotifica.app.model.Comunicacion
import com.mpaznotifica.app.model.EnvioDestinatario
import com.mpaznotifica.app.model.Estudiante
import com.mpaznotifica.app.model.Taller
import com.mpaznotifica.app.model.Usuario
import kotlinx.coroutines.flow.Flow

/**
 * Fuente de datos única que usa el ViewModel. Hay dos implementaciones:
 *
 *  - [FuenteLocal] (modo local): solo Room, con las mismas reglas de negocio del dominio.
 *  - [FuenteServidor] (modo servidor): API (fuente de verdad) + caché Room.
 *
 * [SelectorFuente] elige cuál responde en cada operación según la preferencia [PreferenciaModo].
 * Los resultados usan [ApiResult] en ambos modos para que el ViewModel no distinga el origen.
 */
interface FuenteDatos {
    /** true si esta fuente es la del modo local. */
    val esLocal: Boolean

    val usuarios: Flow<List<Usuario>>
    val talleres: Flow<List<Taller>>
    val estudiantes: Flow<List<Estudiante>>
    val apoderados: Flow<List<Apoderado>>
    val comunicaciones: Flow<List<Comunicacion>>
    val envios: Flow<List<EnvioDestinatario>>

    /** Servicio por el que se despachan los mensajes (simulado en local; POST /mensajes en servidor). */
    fun mensajeria(): MensajeriaApi

    /** Primera apertura: carga los datos de ejemplo en la base de esta fuente. */
    suspend fun asegurarDatosIniciales()

    suspend fun buscarUsuario(correo: String): Usuario?

    /** RF-01. Local: hash con la misma sal contra Room. Servidor: POST /auth/login. */
    suspend fun iniciarSesion(usuario: String, clave: String): ApiResult<SesionRemota>

    suspend fun cerrarSesion(token: String)

    /** Servidor: descarga y actualiza la caché. Local: no hay nada que sincronizar. */
    suspend fun sincronizar(): ApiResult<Unit>

    /** RF-03 a RF-08, RF-11, RF-12, RN-01, RN-02: registra el comunicado ya despachado. */
    suspend fun registrarEnvio(
        comunicacion: Comunicacion,
        destino: String?,
        envios: List<EnvioDestinatario>
    ): ApiResult<ComunicacionRegistrada>

    /** RF-13: [actor] es quien cambia el canal (en el servidor lo identifica el token). */
    suspend fun actualizarCanalPreferente(
        actor: Usuario?,
        apoderadoId: String,
        canal: CanalDespacho
    ): ApiResult<Apoderado>

    /** Solo modo local: borra lo creado y restaura los datos de ejemplo. */
    suspend fun restablecer(): ApiResult<Unit>
}
