package com.mpaznotifica.app.data.repository

import com.mpaznotifica.app.data.remote.ApiResult
import com.mpaznotifica.app.data.remote.CausaError
import com.mpaznotifica.app.data.remote.ComunicacionRegistrada
import com.mpaznotifica.app.data.remote.MensajeriaApi
import com.mpaznotifica.app.data.remote.MensajeriaSimulada
import com.mpaznotifica.app.data.remote.SesionRemota
import com.mpaznotifica.app.domain.Credenciales
import com.mpaznotifica.app.domain.DatosReferencia
import com.mpaznotifica.app.domain.ReglasComunicado
import com.mpaznotifica.app.domain.ValidadorContacto
import com.mpaznotifica.app.model.Apoderado
import com.mpaznotifica.app.model.CanalDespacho
import com.mpaznotifica.app.model.Comunicacion
import com.mpaznotifica.app.model.EnvioDestinatario
import com.mpaznotifica.app.model.ErroresComunicado
import com.mpaznotifica.app.model.ResumenEnvio
import com.mpaznotifica.app.model.Usuario
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * MODO LOCAL: opera solo con Room ([AlmacenLocal]), sin servidor ni red. No duplica reglas: usa las
 * funciones puras del dominio ([ReglasComunicado], [ValidadorContacto], [Credenciales]) que también
 * usa la pantalla, y aplica los mismos rechazos que el servidor (perfil, destino, canal, duplicado),
 * porque la pantalla no es la única barrera.
 *
 * Los datos locales son independientes de los del servidor: lo que se crea aquí no se sincroniza
 * al volver al modo servidor.
 */
class FuenteLocal(
    private val almacen: AlmacenLocal,
    private val mensajeriaLocal: MensajeriaApi = MensajeriaSimulada()
) : FuenteDatos {

    override val esLocal: Boolean = true

    override val usuarios: Flow<List<Usuario>> get() = almacen.usuarios
    override val talleres get() = almacen.talleres
    override val estudiantes get() = almacen.estudiantes
    override val apoderados get() = almacen.apoderados
    override val comunicaciones get() = almacen.comunicaciones
    override val envios get() = almacen.envios

    override fun mensajeria(): MensajeriaApi = mensajeriaLocal

    override suspend fun asegurarDatosIniciales() = almacen.asegurarDatosIniciales()

    override suspend fun buscarUsuario(correo: String): Usuario? = almacen.buscarUsuario(correo)

    // ---------------------------------------------------------------- RF-01

    override suspend fun iniciarSesion(usuario: String, clave: String): ApiResult<SesionRemota> {
        val encontrado = almacen.buscarUsuario(usuario.trim())
        return if (encontrado != null && encontrado.claveHash.isNotEmpty() &&
            Credenciales.coincide(clave, encontrado.claveHash)
        ) {
            ApiResult.Success(SesionRemota(encontrado, TOKEN_LOCAL))
        } else {
            ApiResult.Error("Correo o clave incorrectos", CausaError.NO_AUTORIZADO, 401)
        }
    }

    override suspend fun cerrarSesion(token: String) = Unit

    override suspend fun sincronizar(): ApiResult<Unit> = ApiResult.Success(Unit)

    // ---------------------------------------------------------------- RF-03 a RF-12

    override suspend fun registrarEnvio(
        comunicacion: Comunicacion,
        destino: String?,
        envios: List<EnvioDestinatario>
    ): ApiResult<ComunicacionRegistrada> {
        require(envios.all { it.comunicacionId == comunicacion.id }) { "Los envíos deben pertenecer al comunicado" }

        val autor = almacen.usuarios.first().firstOrNull { it.id == comunicacion.autorId }
            ?: return ApiResult.Error(ReglasComunicado.MSG_SIN_SESION, CausaError.NO_AUTORIZADO, 401)
        val datos = datosActuales()

        // RF-03, RN-01: las mismas reglas de la pantalla, sobre lo que hay en Room.
        val errores = ReglasComunicado.validar(
            comunicacion.asunto,
            ReglasComunicado.sinAviso(comunicacion.cuerpo),
            comunicacion.tipo,
            comunicacion.alcance,
            destino,
            autor,
            datos
        )
        primerError(errores)?.let { return it }

        if (almacen.comunicaciones.first().any { it.id == comunicacion.id }) {
            return ApiResult.Error("Ya existe un comunicado con ese identificador", CausaError.CONFLICTO, 409, "id")
        }

        // Los envíos deben ser exactamente los de los destinatarios, por el canal que corresponde (RF-12).
        val destinatarios = ReglasComunicado.resolverApoderados(comunicacion.alcance, destino, datos)
        val porApoderado = envios.associateBy { it.apoderadoId }
        val coinciden = envios.size == destinatarios.size && destinatarios.all { apoderado ->
            porApoderado[apoderado.id]?.canal == ReglasComunicado.canalDe(apoderado, comunicacion.requiereFirmaFisica)
        }
        if (!coinciden) {
            return ApiResult.Error(
                "Los envíos informados no coinciden con los destinatarios del comunicado",
                CausaError.VALIDACION, 422, "envios"
            )
        }

        return try {
            almacen.guardarRegistro(comunicacion, envios)
            ApiResult.Success(
                ComunicacionRegistrada(comunicacion, envios, ResumenEnvio.de(envios, comunicacion.requiereFirmaFisica))
            )
        } catch (cancelacion: CancellationException) {
            throw cancelacion
        } catch (e: Exception) {
            ApiResult.Error("No se pudieron guardar los datos en el dispositivo.", CausaError.LOCAL)
        }
    }

    // ---------------------------------------------------------------- RF-13

    override suspend fun actualizarCanalPreferente(
        actor: Usuario?,
        apoderadoId: String,
        canal: CanalDespacho
    ): ApiResult<Apoderado> {
        if (actor == null) return ApiResult.Error(ReglasComunicado.MSG_SIN_SESION, CausaError.NO_AUTORIZADO, 401)
        if (!ReglasComunicado.puedeConfigurarCanal(actor)) {
            return ApiResult.Error(
                "Solo Inspectoría General puede cambiar el canal preferente", CausaError.PROHIBIDO, 403
            )
        }
        val apoderado = almacen.apoderados.first().firstOrNull { it.id == apoderadoId }
            ?: return ApiResult.Error("El apoderado no existe", CausaError.NO_ENCONTRADO, 404)
        if (!ValidadorContacto.valido(apoderado, canal)) {
            return ApiResult.Error(
                "${apoderado.nombreCompleto} no tiene un dato válido para ${canal.etiqueta}",
                CausaError.VALIDACION, 422, "canal"
            )
        }
        return try {
            if (almacen.guardarCanal(apoderadoId, canal)) {
                ApiResult.Success(apoderado.copy(canalPreferente = canal))
            } else {
                ApiResult.Error("El apoderado no existe", CausaError.NO_ENCONTRADO, 404)
            }
        } catch (cancelacion: CancellationException) {
            throw cancelacion
        } catch (e: Exception) {
            ApiResult.Error("No se pudo guardar el canal", CausaError.LOCAL)
        }
    }

    // ---------------------------------------------------------------- restablecer

    override suspend fun restablecer(): ApiResult<Unit> =
        try {
            almacen.restablecer()
            ApiResult.Success(Unit)
        } catch (cancelacion: CancellationException) {
            throw cancelacion
        } catch (e: Exception) {
            ApiResult.Error("No se pudieron restablecer los datos locales.", CausaError.LOCAL)
        }

    // ---------------------------------------------------------------- utilidades

    private suspend fun datosActuales(): DatosReferencia {
        val estudiantes = almacen.estudiantes.first()
        return DatosReferencia(
            cursos = estudiantes.map { it.curso }.distinct().sorted(),
            talleres = almacen.talleres.first(),
            estudiantes = estudiantes,
            apoderados = almacen.apoderados.first()
        )
    }

    /** Primer error del formulario, con la causa que daría el servidor (403 perfil, 400/422 formato o regla). */
    private fun primerError(errores: ErroresComunicado): ApiResult.Error? {
        val primero = listOf(
            "asunto" to errores.asunto,
            "cuerpo" to errores.cuerpo,
            "tipo" to errores.tipo,
            "alcance" to errores.alcance,
            "destino" to errores.destino
        ).firstOrNull { it.second != null } ?: return null
        val campo = primero.first
        val mensaje = primero.second!!
        val prohibido = mensaje == ReglasComunicado.MSG_TIPO_NO_DISPONIBLE ||
            mensaje == ReglasComunicado.MSG_SOLO_CURSO ||
            mensaje == ReglasComunicado.MSG_SIN_CURSO ||
            mensaje.startsWith("Tu perfil no puede")
        return if (prohibido) {
            ApiResult.Error(mensaje, CausaError.PROHIBIDO, 403, campo)
        } else {
            ApiResult.Error(mensaje, CausaError.VALIDACION, 400, campo)
        }
    }

    companion object {
        /** Token de la sesión local: no es un secreto, solo marca que no hay sesión en el servidor. */
        const val TOKEN_LOCAL = "sesion-local"
    }
}
