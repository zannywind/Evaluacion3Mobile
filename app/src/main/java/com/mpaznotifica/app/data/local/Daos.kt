package com.mpaznotifica.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {
    @Query("SELECT * FROM usuarios ORDER BY nombre")
    fun observarTodos(): Flow<List<UsuarioEntity>>

    @Query("SELECT * FROM usuarios WHERE LOWER(usuario) = LOWER(:usuario) LIMIT 1")
    suspend fun buscarPorUsuario(usuario: String): UsuarioEntity?

    @Query("SELECT COUNT(*) FROM usuarios")
    suspend fun contar(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarTodos(usuarios: List<UsuarioEntity>)

    /** Sincronización con el servidor: actualiza los datos públicos sin tocar el hash local. */
    @Query("UPDATE usuarios SET nombre = :nombre, usuario = :usuario, perfil = :perfil, cursoAsignado = :cursoAsignado WHERE id = :id")
    suspend fun actualizarDatos(id: String, nombre: String, usuario: String, perfil: String, cursoAsignado: String?): Int
}

@Dao
interface ApoderadoDao {
    @Query("SELECT * FROM apoderados ORDER BY nombreCompleto")
    fun observarTodos(): Flow<List<ApoderadoEntity>>

    /** Único UPDATE permitido sobre estos datos: el canal preferente (RF-13). */
    @Query("UPDATE apoderados SET canalPreferente = :canal WHERE id = :id")
    suspend fun actualizarCanal(id: String, canal: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarTodos(apoderados: List<ApoderadoEntity>)

    /** Sincronización con el servidor: inserta o actualiza (el servidor es la fuente de verdad). */
    @Upsert
    suspend fun upsertTodos(apoderados: List<ApoderadoEntity>)
}

@Dao
interface EstudianteDao {
    @Query("SELECT * FROM estudiantes ORDER BY id")
    fun observarEstudiantes(): Flow<List<EstudianteEntity>>

    @Query("SELECT * FROM talleres ORDER BY nombre")
    fun observarTalleres(): Flow<List<TallerEntity>>

    @Query("SELECT * FROM estudiante_taller")
    fun observarInscripciones(): Flow<List<EstudianteTallerEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarEstudiantes(estudiantes: List<EstudianteEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarTalleres(talleres: List<TallerEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarInscripciones(inscripciones: List<EstudianteTallerEntity>)

    @Upsert
    suspend fun upsertEstudiantes(estudiantes: List<EstudianteEntity>)

    @Upsert
    suspend fun upsertTalleres(talleres: List<TallerEntity>)

    /** Las inscripciones de un estudiante se reemplazan completas al sincronizar. */
    @Query("DELETE FROM estudiante_taller WHERE estudianteId IN (:estudiantesIds)")
    suspend fun borrarInscripcionesDe(estudiantesIds: List<String>)
}

/**
 * Sin @Update ni @Delete de uso en la aplicación: lo enviado es inmutable (RN-02). Los dos DELETE
 * de mantenimiento solo los usa RoomCache.reemplazarBitacora para espejar al servidor.
 */
@Dao
interface ComunicacionDao {
    @Query("SELECT * FROM comunicaciones ORDER BY fechaHora DESC")
    fun observarTodas(): Flow<List<ComunicacionEntity>>

    @Query("SELECT * FROM envios")
    fun observarEnvios(): Flow<List<EnvioEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(comunicacion: ComunicacionEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertarEnvios(envios: List<EnvioEntity>)

    /** Registro ya confirmado por el servidor: si ya estaba en la caché, no se toca. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarOIgnorar(comunicacion: ComunicacionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarEnviosOIgnorar(envios: List<EnvioEntity>)

    @Query("DELETE FROM envios")
    suspend fun borrarEnvios()

    @Query("DELETE FROM comunicaciones")
    suspend fun borrarComunicaciones()
}
