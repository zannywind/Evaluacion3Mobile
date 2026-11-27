package com.mpaznotifica.app.data.remote

import com.mpaznotifica.app.data.repository.ReferenciaRemota
import com.mpaznotifica.app.model.Apoderado
import com.mpaznotifica.app.model.CanalDespacho
import com.mpaznotifica.app.model.Comunicacion
import com.mpaznotifica.app.model.EnvioDestinatario
import com.mpaznotifica.app.model.Usuario

/** Sesión abierta en el servidor: el usuario y su token Bearer. */
data class SesionRemota(val usuario: Usuario, val token: String)

/** Bitácora completa tal como la entrega el servidor. */
data class BitacoraRemota(
    val comunicaciones: List<Comunicacion>,
    val envios: List<EnvioDestinatario>
)

/**
 * EXPERIENCIA 3 - Parte 2 (TODO E3-5): fuente de datos remota.
 *
 * Es la ÚNICA clase que habla con [ApiService]: cada método llama al servicio dentro de
 * [ManejadorErrores.llamar] y convierte los DTO a modelos con los mapeadores (E3-4). El repositorio
 * (Parte 3) usa esta clase y nunca ve DTO ni excepciones.
 *
 * [api] es una función (no una instancia) para que un cambio de dirección del servidor se aplique
 * sin reiniciar: cada llamada pide el servicio vigente.
 *
 * Criterio común: ningún método lanza excepciones (salvo la cancelación de corrutinas); todo
 * fallo sale como [ApiResult.Error] o [ApiResult.SinConexion].
 */
class FuenteRemota(private val api: () -> ApiService) {

    /**
     * TODO(E3-5a): POST /auth/login con `LoginRequestDto(usuario.trim(), clave)`.
     * Criterio: una respuesta sin token o sin usuario es inválida (lanzar
     * [RespuestaInvalidaException] dentro de la llamada). Devuelve [SesionRemota] con el usuario
     * mapeado (E3-4a).
     */
    suspend fun login(usuario: String, clave: String): ApiResult<SesionRemota> =
        TODO("E3-5a: ManejadorErrores.llamar { api().login(...) } -> SesionRemota")

    /**
     * TODO(E3-5b): POST /auth/logout enviando `"Bearer $token"` en el encabezado Authorization.
     * Criterio: devuelve el [ApiResult] de la llamada; el llamador ignorará un fallo.
     */
    suspend fun logout(token: String): ApiResult<Unit> =
        TODO("E3-5b: ManejadorErrores.llamar { api().logout(\"Bearer \$token\") }")

    /**
     * TODO(E3-5c): descarga los datos de referencia: GET /usuarios, /talleres, /estudiantes y
     * /apoderados (sin filtrar por curso: pasar null) y los convierte a [ReferenciaRemota]
     * (`EstudianteRemoto` conserva el `apoderadoId` del DTO, que no puede venir vacío).
     * Criterio: todo o nada; si una de las cuatro llamadas falla, devuelve ese error y no una
     * referencia parcial.
     */
    suspend fun descargarReferencia(): ApiResult<ReferenciaRemota> =
        TODO("E3-5c: cuatro llamadas con ManejadorErrores.llamar; cortar en el primer fallo")

    /**
     * TODO(E3-5d): descarga la bitácora: GET /comunicaciones (sin filtros: null en los cuatro
     * parámetros) y GET /envios (sin filtro). Mismo criterio de todo o nada.
     */
    suspend fun descargarBitacora(): ApiResult<BitacoraRemota> =
        TODO("E3-5d: dos llamadas; devolver BitacoraRemota")

    /**
     * TODO(E3-5e): POST /comunicaciones con el cuerpo `comunicacion.aNuevaDto(destino, envios)`
     * (E3-4i) y respuesta convertida con `aDominio()` (E3-4j). Un 409 (id repetido) debe llegar como
     * `CausaError.CONFLICTO`; un 403 como `PROHIBIDO` (RN-01).
     */
    suspend fun crearComunicacion(
        comunicacion: Comunicacion,
        destino: String?,
        envios: List<EnvioDestinatario>
    ): ApiResult<ComunicacionRegistrada> =
        TODO("E3-5e: POST /comunicaciones")

    /**
     * TODO(E3-5f): PATCH /apoderados/{id}/canal con `CanalRequestDto(canal.name)`; devuelve el
     * apoderado actualizado (E3-4d). Un 403 (perfil sin permiso) y un 422 (sin contacto válido)
     * deben llegar como [ApiResult.Error] con el mensaje del servidor.
     */
    suspend fun actualizarCanal(apoderadoId: String, canal: CanalDespacho): ApiResult<Apoderado> =
        TODO("E3-5f: PATCH /apoderados/{id}/canal")
}
