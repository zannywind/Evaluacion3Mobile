package com.mpaznotifica.app.data.repository

import androidx.room.withTransaction
import com.mpaznotifica.app.data.MockData
import com.mpaznotifica.app.data.local.AppDatabase
import com.mpaznotifica.app.data.local.EstudianteEntity
import com.mpaznotifica.app.data.local.EstudianteTallerEntity
import com.mpaznotifica.app.data.local.TallerEntity
import com.mpaznotifica.app.data.local.aEntidad
import com.mpaznotifica.app.data.local.aModelo
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
 * Caché local en Room: la pantalla siempre lee de aquí (Flow), y [MpazRepository] la mantiene al
 * día con lo que entrega el servidor. Sin conexión, la app sigue mostrando lo último sincronizado.
 */
class RoomCache(private val db: AppDatabase) : AlmacenLocal {

    private val usuarioDao = db.usuarioDao()
    private val apoderadoDao = db.apoderadoDao()
    private val estudianteDao = db.estudianteDao()
    private val comunicacionDao = db.comunicacionDao()

    private val candadoSembrado = Mutex()
    private var sembrada = false

    override val usuarios: Flow<List<Usuario>> =
        usuarioDao.observarTodos().map { lista -> lista.map { it.aModelo() } }

    override val talleres: Flow<List<Taller>> =
        estudianteDao.observarTalleres().map { lista -> lista.map { Taller(it.id, it.nombre) } }

    override val estudiantes: Flow<List<Estudiante>> =
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

    override val apoderados: Flow<List<Apoderado>> =
        combine(apoderadoDao.observarTodos(), estudianteDao.observarEstudiantes()) { filas, estudiantes ->
            filas.map { fila ->
                fila.aModelo(estudiantes.filter { it.apoderadoId == fila.id }.map { it.id })
            }
        }

    override val comunicaciones: Flow<List<Comunicacion>> =
        comunicacionDao.observarTodas().map { lista -> lista.map { it.aModelo() } }

    override val envios: Flow<List<EnvioDestinatario>> =
        comunicacionDao.observarEnvios().map { lista -> lista.map { it.aModelo() } }

    override suspend fun asegurarDatosIniciales() {
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

    /**
     * Modo local: deja la base como en la primera apertura. La bitácora se vacía y se vuelve a
     * cargar desde MockData (con los disparadores de RN-02 quitados solo durante la transacción);
     * los apoderados vuelven a su canal preferente de ejemplo.
     */
    override suspend fun restablecer() {
        candadoSembrado.withLock {
            db.withTransaction {
                val base = db.openHelper.writableDatabase
                AppDatabase.quitarProteccionBorrado(base)
                comunicacionDao.borrarEnvios()
                comunicacionDao.borrarComunicaciones()
                AppDatabase.crearProteccion(base)
                apoderadoDao.upsertTodos(MockData.apoderados.map { it.aEntidad() })
                MockData.comunicados.forEach { comunicacionDao.insertar(it.aEntidad()) }
                comunicacionDao.insertarEnvios(MockData.envios.map { it.aEntidad() })
            }
            sembrada = true
        }
    }

    override suspend fun buscarUsuario(correo: String): Usuario? =
        usuarioDao.buscarPorUsuario(correo.trim())?.aModelo()

    override suspend fun guardarReferencia(referencia: ReferenciaRemota) {
        db.withTransaction {
            // Usuarios: se actualizan los datos públicos y se conserva el hash local ya existente.
            for (usuario in referencia.usuarios) {
                val actualizados = usuarioDao.actualizarDatos(
                    usuario.id, usuario.nombre, usuario.usuario, usuario.perfil.name, usuario.cursoAsignado
                )
                if (actualizados == 0) usuarioDao.insertarTodos(listOf(usuario.aEntidad()))
            }
            apoderadoDao.upsertTodos(referencia.apoderados.map { it.aEntidad() })
            estudianteDao.upsertTalleres(referencia.talleres.map { TallerEntity(it.id, it.nombre) })
            estudianteDao.upsertEstudiantes(
                referencia.estudiantes.map {
                    EstudianteEntity(
                        it.estudiante.id, it.estudiante.nombreReferencial, it.estudiante.curso, it.apoderadoId
                    )
                }
            )
            val ids = referencia.estudiantes.map { it.estudiante.id }
            ids.chunked(500).forEach { estudianteDao.borrarInscripcionesDe(it) }
            estudianteDao.insertarInscripciones(
                referencia.estudiantes.flatMap { r ->
                    r.estudiante.talleresIds.map { EstudianteTallerEntity(r.estudiante.id, it) }
                }
            )
        }
    }

    override suspend fun reemplazarBitacora(comunicaciones: List<Comunicacion>, envios: List<EnvioDestinatario>) {
        db.withTransaction {
            // Mantenimiento de la copia local: se quitan un momento los disparadores de RN-02 para
            // espejar al servidor (fuente de verdad). Si algo falla, la transacción se revierte
            // completa y los disparadores quedan intactos.
            val base = db.openHelper.writableDatabase
            AppDatabase.quitarProteccionBorrado(base)
            comunicacionDao.borrarEnvios()
            comunicacionDao.borrarComunicaciones()
            AppDatabase.crearProteccion(base)
            comunicaciones.forEach { comunicacionDao.insertar(it.aEntidad()) }
            comunicacionDao.insertarEnvios(envios.map { it.aEntidad() })
        }
    }

    override suspend fun guardarRegistro(comunicacion: Comunicacion, envios: List<EnvioDestinatario>) {
        db.withTransaction {
            comunicacionDao.insertarOIgnorar(comunicacion.aEntidad())
            comunicacionDao.insertarEnviosOIgnorar(envios.map { it.aEntidad() })
        }
    }

    override suspend fun guardarCanal(apoderadoId: String, canal: CanalDespacho): Boolean =
        apoderadoDao.actualizarCanal(apoderadoId, canal.name) > 0
}
