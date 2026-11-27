package com.mpaznotifica.app.data.repository

import androidx.room.withTransaction
import com.mpaznotifica.app.data.MockData
import com.mpaznotifica.app.data.local.AppDatabase
import com.mpaznotifica.app.data.local.EstudianteEntity
import com.mpaznotifica.app.data.local.EstudianteTallerEntity
import com.mpaznotifica.app.data.local.TallerEntity
import com.mpaznotifica.app.data.local.aEntidad
import com.mpaznotifica.app.data.local.aModelo
import com.mpaznotifica.app.data.remote.ApiResult
import com.mpaznotifica.app.data.remote.ComunicacionRegistrada
import com.mpaznotifica.app.data.remote.FuenteRemota
import com.mpaznotifica.app.data.remote.SesionRemota
import com.mpaznotifica.app.model.Apoderado
import com.mpaznotifica.app.model.CanalDespacho
import com.mpaznotifica.app.model.Comunicacion
import com.mpaznotifica.app.model.EnvioDestinatario
import com.mpaznotifica.app.model.Estudiante
import com.mpaznotifica.app.model.Taller
import com.mpaznotifica.app.model.Usuario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Capa Repository: único punto de acceso a los datos para el ViewModel.
 *
 * Modo local:     UI -> ViewModel -> SelectorFuente -> FuenteLocal -> Room (base local).
 * Modo servidor:  UI -> ViewModel -> SelectorFuente -> FuenteServidor -> Repository -> API (fuente de
 *                 verdad) -> Room (caché) -> UI.
 *
 * El modo se elige en tiempo de ejecución (BuildConfig.USAR_API solo da el valor inicial): esta clase
 * es la ruta del modo servidor; el modo local lo atiende FuenteLocal con su propia base. Los métodos
 * de modo local de esta clase (registrarEnvio, actualizarCanalPreferente) están completos y no se tocan. Los de modo API (sección "Experiencia
 * 3", al final) son los TODO(E3-6 a E3-8) de la Parte 3.
 */
class MpazRepository(
    private val db: AppDatabase,
    private val remoto: FuenteRemota? = null,
    cacheLocal: CacheLocal? = null
) {

    private val usuarioDao = db.usuarioDao()
    private val apoderadoDao = db.apoderadoDao()
    private val estudianteDao = db.estudianteDao()
    private val comunicacionDao = db.comunicacionDao()

    private val candadoSembrado = Mutex()
    private var sembrada = false

    val usuarios: Flow<List<Usuario>> =
        usuarioDao.observarTodos().map { lista -> lista.map { it.aModelo() } }

    val talleres: Flow<List<Taller>> =
        estudianteDao.observarTalleres().map { lista -> lista.map { Taller(it.id, it.nombre) } }

    val estudiantes: Flow<List<Estudiante>> =
        combine(estudianteDao.observarEstudiantes(), estudianteDao.observarInscripciones()) { filas, inscripciones ->
            filas.map { fila ->
                Estudiante(
                    id = fila.id,
                    nombreReferencial = fila.nombreReferencial,
                    curso = fila.curso,
                    talleresIds = inscripciones.filter { it.estudianteId == fila.id }.map { it.tallerId }
                )
            }
        }

    val apoderados: Flow<List<Apoderado>> =
        combine(apoderadoDao.observarTodos(), estudianteDao.observarEstudiantes()) { filas, estudiantes ->
            filas.map { fila ->
                fila.aModelo(estudiantes.filter { it.apoderadoId == fila.id }.map { it.id })
            }
        }

    val comunicaciones: Flow<List<Comunicacion>> =
        comunicacionDao.observarTodas().map { lista -> lista.map { it.aModelo() } }

    val envios: Flow<List<EnvioDestinatario>> =
        comunicacionDao.observarEnvios().map { lista -> lista.map { it.aModelo() } }

    /** Carga la base sintética (MockData) la primera vez que se abre la app. */
    suspend fun asegurarDatosIniciales() {
        candadoSembrado.withLock {
            if (sembrada) return
            if (usuarioDao.contar() == 0) {
                db.withTransaction {
                    usuarioDao.insertarTodos(MockData.usuarios.map { it.aEntidad() })
                    apoderadoDao.insertarTodos(MockData.apoderados.map { it.aEntidad() })
                    estudianteDao.insertarTalleres(MockData.talleres.map { TallerEntity(it.id, it.nombre) })
                    estudianteDao.insertarEstudiantes(
                        MockData.estudiantes.map { estudiante ->
                            val apoderado = MockData.apoderados.first { estudiante.id in it.pupilosIds }
                            EstudianteEntity(estudiante.id, estudiante.nombreReferencial, estudiante.curso, apoderado.id)
                        }
                    )
                    estudianteDao.insertarInscripciones(
                        MockData.estudiantes.flatMap { e -> e.talleresIds.map { EstudianteTallerEntity(e.id, it) } }
                    )
                    MockData.comunicados.forEach { comunicacionDao.insertar(it.aEntidad()) }
                    comunicacionDao.insertarEnvios(MockData.envios.map { it.aEntidad() })
                }
            }
            sembrada = true
        }
    }

    suspend fun buscarUsuario(correo: String): Usuario? =
        usuarioDao.buscarPorUsuario(correo.trim())?.aModelo()

    /**
     * Guarda el comunicado (inmutable, RN-02) junto con el resultado del despacho a cada
     * destinatario, en una sola transacción: no quedan registros incompletos.
     */
    suspend fun registrarEnvio(comunicacion: Comunicacion, envios: List<EnvioDestinatario>) {
        require(envios.all { it.comunicacionId == comunicacion.id }) {
            "Los envíos deben pertenecer al comunicado"
        }
        db.withTransaction {
            comunicacionDao.insertar(comunicacion.aEntidad())
            comunicacionDao.insertarEnvios(envios.map { it.aEntidad() })
        }
    }

    /** RF-13: actualiza el canal preferente. Devuelve false si el apoderado no existe. */
    suspend fun actualizarCanalPreferente(apoderadoId: String, canal: CanalDespacho): Boolean =
        apoderadoDao.actualizarCanal(apoderadoId, canal.name) > 0

    // =====================================================================================
    // EXPERIENCIA 3 - Parte 3: integración con la API (solo se usa en modo servidor)
    // =====================================================================================
    //
    // Reglas de la arquitectura (criterio común de E3-6 a E3-8):
    //  - LECTURA: la API es la fuente de verdad. Se descarga con [remoto], se guarda en Room con
    //    [cache] y las pantallas siguen observando los Flow de Room (los mismos de arriba).
    //  - ESCRITURA: la API va primero. La caché local solo cambia cuando el servidor confirma.
    //  - SIN RED: si el servidor no responde, la caché NO se toca y se devuelve el resultado de
    //    error ([ApiResult.SinConexion]) para que el ViewModel avise y siga mostrando lo guardado.
    //  - Si falla el guardado en Room, devolver ApiResult.Error(..., CausaError.LOCAL) con un
    //    mensaje en español (el servidor ya tiene el dato: se verá en la próxima sincronización).

    /**
     * Caché local (Room) de lo que entrega el servidor. Ya implementada: ver RoomCache. El parámetro
     * [cacheLocal] del constructor permite reemplazarla por un doble (MockK) en las pruebas JVM.
     */
    private val cache: CacheLocal by lazy { cacheLocal ?: RoomCache(db) }

    /**
     * TODO(E3-8a): RF-01. Inicia sesión en el servidor (POST /auth/login) a través de [remoto].
     * Criterio: devuelve la sesión (usuario + token) o el error; con credenciales incorrectas el
     * resultado es `ApiResult.Error` con causa NO_AUTORIZADO. No guardar la clave en ningún lado.
     */
    suspend fun iniciarSesionRemoto(usuario: String, clave: String): ApiResult<SesionRemota> =
        TODO("E3-8a: delegar en remoto!!.login(usuario, clave)")

    /**
     * TODO(E3-8b): cierra la sesión en el servidor (POST /auth/logout). Si falla (por ejemplo sin
     * red) no importa: el token ya se descartó en la app. No debe lanzar excepciones.
     */
    suspend fun cerrarSesionRemota(token: String) {
        TODO("E3-8b: remoto!!.logout(token) ignorando el resultado")
    }

    /**
     * TODO(E3-6): RF-04, RF-09. Descarga referencia y bitácora del servidor y deja Room igual.
     * Criterios de aceptación:
     *  - Usa `remoto.descargarReferencia()` y `remoto.descargarBitacora()`; si cualquiera falla,
     *    devuelve ese error y NO modifica Room (todo o nada).
     *  - Si ambas funcionan: `cache.guardarReferencia(...)` y luego `cache.reemplazarBitacora(...)`;
     *    devuelve `ApiResult.Success(Unit)`.
     *  - Si Room lanza una excepción (que no sea cancelación), devuelve
     *    `ApiResult.Error("No se pudieron guardar los datos en el dispositivo.", CausaError.LOCAL)`.
     */
    suspend fun sincronizar(): ApiResult<Unit> =
        TODO("E3-6: descargar -> guardar en Room -> Success(Unit); error/sin red -> caché intacta")

    /**
     * TODO(E3-7a): RF-03 a RF-08, RF-11, RF-12, RN-01, RN-02. Registra en el servidor el comunicado
     * ya despachado (POST /comunicaciones) y, solo si el servidor lo confirma, guarda en Room el
     * registro que ÉL devolvió (con su autor, fecha y hora definitivos) usando `cache.guardarRegistro`.
     * Criterios: `require` que todos los envíos pertenezcan al comunicado; si el servidor rechaza
     * (403, 400, 422, 409) o no responde, Room no cambia y se devuelve el error; si Room falla
     * DESPUÉS de la confirmación del servidor, igual se devuelve el éxito (el dato ya está en el servidor).
     */
    suspend fun registrarEnvioRemoto(
        comunicacion: Comunicacion,
        destino: String?,
        envios: List<EnvioDestinatario>
    ): ApiResult<ComunicacionRegistrada> =
        TODO("E3-7a: remoto.crearComunicacion(...) y luego cache.guardarRegistro(...) solo si hubo éxito")

    /**
     * TODO(E3-7b): RF-13. Cambia el canal preferente en el servidor (PATCH /apoderados/{id}/canal)
     * y, solo si lo acepta, en Room con `cache.guardarCanal`. Criterios: un 403 (perfil sin
     * permiso) o un 422 (sin contacto válido para ese canal) dejan Room intacto y devuelven el error.
     */
    suspend fun actualizarCanalRemoto(apoderadoId: String, canal: CanalDespacho): ApiResult<Apoderado> =
        TODO("E3-7b: remoto.actualizarCanal(...) y luego cache.guardarCanal(...) solo si hubo éxito")
}
