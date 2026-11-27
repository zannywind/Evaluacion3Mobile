package com.mpaznotifica.app.modo

import com.mpaznotifica.app.data.MockData
import com.mpaznotifica.app.data.remote.ApiResult
import com.mpaznotifica.app.data.remote.ComunicacionRegistrada
import com.mpaznotifica.app.data.remote.MensajeDto
import com.mpaznotifica.app.data.remote.MensajeriaApi
import com.mpaznotifica.app.data.remote.RespuestaEnvioDto
import com.mpaznotifica.app.data.remote.SesionRemota
import com.mpaznotifica.app.data.repository.AlmacenLocal
import com.mpaznotifica.app.data.repository.FuenteDatos
import com.mpaznotifica.app.data.repository.ReferenciaRemota
import com.mpaznotifica.app.model.Apoderado
import com.mpaznotifica.app.model.CanalDespacho
import com.mpaznotifica.app.model.Comunicacion
import com.mpaznotifica.app.model.EnvioDestinatario
import com.mpaznotifica.app.model.Estudiante
import com.mpaznotifica.app.model.ResumenEnvio
import com.mpaznotifica.app.model.Taller
import com.mpaznotifica.app.model.Usuario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Base local en memoria: se comporta como Room (inserta, actualiza el canal, restablece al ejemplo). */
class AlmacenFalso : AlmacenLocal {
    val usuariosEstado = MutableStateFlow<List<Usuario>>(MockData.usuarios)
    val apoderadosEstado = MutableStateFlow<List<Apoderado>>(MockData.apoderados)
    val comunicacionesEstado = MutableStateFlow<List<Comunicacion>>(MockData.comunicados)
    val enviosEstado = MutableStateFlow<List<EnvioDestinatario>>(MockData.envios)

    var fallarAlGuardar = false
    var restablecimientos = 0

    override val usuarios: Flow<List<Usuario>> = usuariosEstado
    override val talleres: Flow<List<Taller>> = MutableStateFlow(MockData.talleres)
    override val estudiantes: Flow<List<Estudiante>> = MutableStateFlow(MockData.estudiantes)
    override val apoderados: Flow<List<Apoderado>> = apoderadosEstado
    override val comunicaciones: Flow<List<Comunicacion>> = comunicacionesEstado
    override val envios: Flow<List<EnvioDestinatario>> = enviosEstado

    override suspend fun asegurarDatosIniciales() = Unit

    override suspend fun buscarUsuario(correo: String): Usuario? =
        usuariosEstado.value.firstOrNull { it.usuario.equals(correo.trim(), ignoreCase = true) }

    override suspend fun guardarReferencia(referencia: ReferenciaRemota) = Unit

    override suspend fun reemplazarBitacora(comunicaciones: List<Comunicacion>, envios: List<EnvioDestinatario>) = Unit

    override suspend fun guardarRegistro(comunicacion: Comunicacion, envios: List<EnvioDestinatario>) {
        if (fallarAlGuardar) throw IllegalStateException("Room falló")
        comunicacionesEstado.value = comunicacionesEstado.value + comunicacion
        enviosEstado.value = enviosEstado.value + envios
    }

    override suspend fun guardarCanal(apoderadoId: String, canal: CanalDespacho): Boolean {
        if (apoderadosEstado.value.none { it.id == apoderadoId }) return false
        apoderadosEstado.value = apoderadosEstado.value.map {
            if (it.id == apoderadoId) it.copy(canalPreferente = canal) else it
        }
        return true
    }

    override suspend fun restablecer() {
        restablecimientos++
        apoderadosEstado.value = MockData.apoderados
        comunicacionesEstado.value = MockData.comunicados
        enviosEstado.value = MockData.envios
    }
}

/** Mensajería que siempre despacha, sin esperar (para no depender de la latencia simulada). */
class MensajeriaInstantanea : MensajeriaApi {
    val recibidos = mutableListOf<MensajeDto>()
    override suspend fun enviar(mensaje: MensajeDto): RespuestaEnvioDto {
        recibidos += mensaje
        return RespuestaEnvioDto(true, "ok")
    }
}

/**
 * Fuente de datos de mentira: anota qué operaciones recibió, para comprobar que el selector elige
 * la fuente correcta. [nombre] identifica de cuál se trata.
 */
class FuenteFalsa(val nombre: String, override val esLocal: Boolean) : FuenteDatos {
    val llamadas = mutableListOf<String>()
    val mensajeriaPropia = MensajeriaInstantanea()

    val usuariosEstado = MutableStateFlow(listOf(Usuario("U-$nombre", nombre, "$nombre@ejemplo.cl", "", com.mpaznotifica.app.model.Perfil.INSPECTORIA_GENERAL)))

    override val usuarios: Flow<List<Usuario>> = usuariosEstado
    override val talleres: Flow<List<Taller>> = MutableStateFlow(emptyList())
    override val estudiantes: Flow<List<Estudiante>> = MutableStateFlow(emptyList())
    override val apoderados: Flow<List<Apoderado>> = MutableStateFlow(emptyList())
    override val comunicaciones: Flow<List<Comunicacion>> = MutableStateFlow(emptyList())
    override val envios: Flow<List<EnvioDestinatario>> = MutableStateFlow(emptyList())

    override fun mensajeria(): MensajeriaApi {
        llamadas += "mensajeria"
        return mensajeriaPropia
    }

    override suspend fun asegurarDatosIniciales() {
        llamadas += "asegurar"
    }

    override suspend fun buscarUsuario(correo: String): Usuario? {
        llamadas += "buscarUsuario"
        return null
    }

    override suspend fun iniciarSesion(usuario: String, clave: String): ApiResult<SesionRemota> {
        llamadas += "login"
        return ApiResult.Success(SesionRemota(usuariosEstado.value.first(), "token-$nombre"))
    }

    override suspend fun cerrarSesion(token: String) {
        llamadas += "logout"
    }

    override suspend fun sincronizar(): ApiResult<Unit> {
        llamadas += "sincronizar"
        return ApiResult.Success(Unit)
    }

    override suspend fun registrarEnvio(
        comunicacion: Comunicacion,
        destino: String?,
        envios: List<EnvioDestinatario>
    ): ApiResult<ComunicacionRegistrada> {
        llamadas += "registrar"
        return ApiResult.Success(ComunicacionRegistrada(comunicacion, envios, ResumenEnvio.de(envios)))
    }

    override suspend fun actualizarCanalPreferente(
        actor: Usuario?,
        apoderadoId: String,
        canal: CanalDespacho
    ): ApiResult<Apoderado> {
        llamadas += "canal"
        return ApiResult.Success(MockData.apoderados.first { it.id == apoderadoId }.copy(canalPreferente = canal))
    }

    override suspend fun restablecer(): ApiResult<Unit> {
        llamadas += "restablecer"
        return ApiResult.Success(Unit)
    }
}
