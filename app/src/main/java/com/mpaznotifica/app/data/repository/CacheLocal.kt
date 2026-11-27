package com.mpaznotifica.app.data.repository

import com.mpaznotifica.app.model.Apoderado
import com.mpaznotifica.app.model.CanalDespacho
import com.mpaznotifica.app.model.Comunicacion
import com.mpaznotifica.app.model.EnvioDestinatario
import com.mpaznotifica.app.model.Estudiante
import com.mpaznotifica.app.model.Taller
import com.mpaznotifica.app.model.Usuario
import kotlinx.coroutines.flow.Flow

/** Estudiante tal como lo entrega el servidor: incluye su apoderado (clave foránea en Room). */
data class EstudianteRemoto(val estudiante: Estudiante, val apoderadoId: String)

/** Datos de referencia descargados del servidor. */
data class ReferenciaRemota(
    val usuarios: List<Usuario>,
    val talleres: List<Taller>,
    val estudiantes: List<EstudianteRemoto>,
    val apoderados: List<Apoderado>
)

/**
 * Caché local (Room) de lo que el servidor entrega. La interfaz permite probar el repositorio en
 * JVM con una implementación en memoria; la real es [RoomCache].
 */
interface CacheLocal {
    val usuarios: Flow<List<Usuario>>
    val talleres: Flow<List<Taller>>
    val estudiantes: Flow<List<Estudiante>>
    val apoderados: Flow<List<Apoderado>>
    val comunicaciones: Flow<List<Comunicacion>>
    val envios: Flow<List<EnvioDestinatario>>

    /** Primera apertura: deja una copia inicial de los datos de demostración para ver algo sin red. */
    suspend fun asegurarDatosIniciales()

    suspend fun buscarUsuario(correo: String): Usuario?

    /** Inserta o actualiza los datos de referencia (sin tocar la clave local de los usuarios). */
    suspend fun guardarReferencia(referencia: ReferenciaRemota)

    /** Deja la bitácora local idéntica a la del servidor. */
    suspend fun reemplazarBitacora(comunicaciones: List<Comunicacion>, envios: List<EnvioDestinatario>)

    /** Agrega un comunicado recién registrado en el servidor. */
    suspend fun guardarRegistro(comunicacion: Comunicacion, envios: List<EnvioDestinatario>)

    suspend fun guardarCanal(apoderadoId: String, canal: CanalDespacho): Boolean
}

/**
 * Base de datos del MODO LOCAL: una [CacheLocal] que además se puede restablecer. Es una base
 * distinta de la caché del servidor (otro archivo Room): lo creado en modo local no se mezcla con
 * lo del servidor ni se sincroniza con él.
 */
interface AlmacenLocal : CacheLocal {
    /** Borra lo creado en modo local (comunicados y envíos) y restaura los datos de ejemplo. */
    suspend fun restablecer()
}
