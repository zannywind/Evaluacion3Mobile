package com.mpaznotifica.app.data.repository

import com.mpaznotifica.app.data.remote.ApiResult
import com.mpaznotifica.app.data.remote.CausaError
import com.mpaznotifica.app.data.remote.ComunicacionRegistrada
import com.mpaznotifica.app.data.remote.MensajeriaApi
import com.mpaznotifica.app.data.remote.SesionRemota
import com.mpaznotifica.app.model.Apoderado
import com.mpaznotifica.app.model.CanalDespacho
import com.mpaznotifica.app.model.Comunicacion
import com.mpaznotifica.app.model.EnvioDestinatario
import com.mpaznotifica.app.model.Usuario

/**
 * MODO SERVIDOR: la ruta de la Experiencia 3. La API es la fuente de verdad y Room (otra base,
 * distinta de la del modo local) es la caché que observan las pantallas. Solo delega en
 * [MpazRepository]: lo que haga falta aquí depende de los TODO(E3-n) de ese repositorio y de
 * [com.mpaznotifica.app.data.remote.FuenteRemota]; esta clase no los completa.
 */
class FuenteServidor(
    private val repositorio: MpazRepository,
    private val mensajeriaApi: () -> MensajeriaApi
) : FuenteDatos {

    override val esLocal: Boolean = false

    override val usuarios get() = repositorio.usuarios
    override val talleres get() = repositorio.talleres
    override val estudiantes get() = repositorio.estudiantes
    override val apoderados get() = repositorio.apoderados
    override val comunicaciones get() = repositorio.comunicaciones
    override val envios get() = repositorio.envios

    override fun mensajeria(): MensajeriaApi = mensajeriaApi()

    override suspend fun asegurarDatosIniciales() = repositorio.asegurarDatosIniciales()

    override suspend fun buscarUsuario(correo: String): Usuario? = repositorio.buscarUsuario(correo)

    override suspend fun iniciarSesion(usuario: String, clave: String): ApiResult<SesionRemota> =
        repositorio.iniciarSesionRemoto(usuario, clave)

    override suspend fun cerrarSesion(token: String) = repositorio.cerrarSesionRemota(token)

    override suspend fun sincronizar(): ApiResult<Unit> = repositorio.sincronizar()

    override suspend fun registrarEnvio(
        comunicacion: Comunicacion,
        destino: String?,
        envios: List<EnvioDestinatario>
    ): ApiResult<ComunicacionRegistrada> = repositorio.registrarEnvioRemoto(comunicacion, destino, envios)

    override suspend fun actualizarCanalPreferente(
        actor: Usuario?,
        apoderadoId: String,
        canal: CanalDespacho
    ): ApiResult<Apoderado> = repositorio.actualizarCanalRemoto(apoderadoId, canal)

    override suspend fun restablecer(): ApiResult<Unit> =
        ApiResult.Error("Restablecer solo está disponible en el modo local.", CausaError.LOCAL)
}
