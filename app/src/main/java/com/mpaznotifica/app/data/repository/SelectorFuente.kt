package com.mpaznotifica.app.data.repository

import com.mpaznotifica.app.data.remote.ApiResult
import com.mpaznotifica.app.data.remote.ComunicacionRegistrada
import com.mpaznotifica.app.data.remote.MensajeriaApi
import com.mpaznotifica.app.data.remote.SesionRemota
import com.mpaznotifica.app.model.Apoderado
import com.mpaznotifica.app.model.CanalDespacho
import com.mpaznotifica.app.model.Comunicacion
import com.mpaznotifica.app.model.EnvioDestinatario
import com.mpaznotifica.app.model.Usuario
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest

/**
 * Selector de fuente: la ÚNICA interfaz de datos que ve el ViewModel. En cada operación consulta
 * la preferencia ([PreferenciaModo.esLocal]) y delega en [local] o en [servidor]; los Flow que
 * observan las pantallas cambian de base en caliente cuando cambia el modo (flatMapLatest).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SelectorFuente(
    private val preferencia: PreferenciaModo,
    private val local: FuenteDatos,
    private val servidor: FuenteDatos
) : FuenteDatos {

    /** Fuente que corresponde AHORA según la preferencia. */
    fun actual(): FuenteDatos = if (preferencia.esLocal()) local else servidor

    override val esLocal: Boolean get() = preferencia.esLocal()

    private fun <T> observar(leer: (FuenteDatos) -> Flow<T>): Flow<T> =
        preferencia.modoLocal.flatMapLatest { enLocal -> leer(if (enLocal) local else servidor) }

    override val usuarios: Flow<List<Usuario>> = observar { it.usuarios }
    override val talleres = observar { it.talleres }
    override val estudiantes = observar { it.estudiantes }
    override val apoderados = observar { it.apoderados }
    override val comunicaciones: Flow<List<Comunicacion>> = observar { it.comunicaciones }
    override val envios: Flow<List<EnvioDestinatario>> = observar { it.envios }

    override fun mensajeria(): MensajeriaApi = actual().mensajeria()

    /** Deja listas las dos bases (la local y la caché del servidor) para poder cambiar de modo sin esperas. */
    override suspend fun asegurarDatosIniciales() {
        local.asegurarDatosIniciales()
        servidor.asegurarDatosIniciales()
    }

    override suspend fun buscarUsuario(correo: String): Usuario? = actual().buscarUsuario(correo)

    override suspend fun iniciarSesion(usuario: String, clave: String): ApiResult<SesionRemota> =
        actual().iniciarSesion(usuario, clave)

    override suspend fun cerrarSesion(token: String) = actual().cerrarSesion(token)

    override suspend fun sincronizar(): ApiResult<Unit> = actual().sincronizar()

    override suspend fun registrarEnvio(
        comunicacion: Comunicacion,
        destino: String?,
        envios: List<EnvioDestinatario>
    ): ApiResult<ComunicacionRegistrada> = actual().registrarEnvio(comunicacion, destino, envios)

    override suspend fun actualizarCanalPreferente(
        actor: Usuario?,
        apoderadoId: String,
        canal: CanalDespacho
    ): ApiResult<Apoderado> = actual().actualizarCanalPreferente(actor, apoderadoId, canal)

    override suspend fun restablecer(): ApiResult<Unit> = actual().restablecer()
}
