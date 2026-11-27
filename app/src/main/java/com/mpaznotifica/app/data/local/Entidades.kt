package com.mpaznotifica.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/*
 * Modelo relacional (Room / SQLite). Los enums se guardan como texto (enum.name).
 *
 * Supuestos pendientes de confirmar con el CITT:
 *  - Un apoderado puede tener más de un pupilo: estudiantes.apoderadoId (relación 1..N).
 *  - Los talleres son una entidad propia con estudiantes inscritos: tabla intermedia N..N.
 */

@Entity(
    tableName = "usuarios",
    indices = [Index(value = ["usuario"], unique = true)]
)
data class UsuarioEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val usuario: String,
    val claveHash: String,
    val perfil: String,
    val cursoAsignado: String?
)

@Entity(tableName = "apoderados")
data class ApoderadoEntity(
    @PrimaryKey val id: String,
    val nombreCompleto: String,
    val correo: String,
    val telefono: String,
    val canalPreferente: String
)

@Entity(
    tableName = "estudiantes",
    foreignKeys = [
        ForeignKey(
            entity = ApoderadoEntity::class,
            parentColumns = ["id"],
            childColumns = ["apoderadoId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("apoderadoId")]
)
data class EstudianteEntity(
    @PrimaryKey val id: String,
    val nombreReferencial: String,
    val curso: String,
    val apoderadoId: String
)

@Entity(tableName = "talleres")
data class TallerEntity(
    @PrimaryKey val id: String,
    val nombre: String
)

/** Tabla intermedia estudiante N..N taller. */
@Entity(
    tableName = "estudiante_taller",
    primaryKeys = ["estudianteId", "tallerId"],
    foreignKeys = [
        ForeignKey(
            entity = EstudianteEntity::class,
            parentColumns = ["id"],
            childColumns = ["estudianteId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = TallerEntity::class,
            parentColumns = ["id"],
            childColumns = ["tallerId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("tallerId")]
)
data class EstudianteTallerEntity(
    val estudianteId: String,
    val tallerId: String
)

/**
 * Comunicado emitido. INMUTABLE (RN-02): el DAO solo tiene inserción y consultas, y la base
 * tiene disparadores que rechazan cualquier UPDATE o DELETE (ver AppDatabase).
 */
@Entity(
    tableName = "comunicaciones",
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["autorId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("autorId"), Index("fechaHora")]
)
data class ComunicacionEntity(
    @PrimaryKey val id: String,
    val asunto: String,
    val cuerpo: String,
    val tipo: String,
    val alcance: String,
    val destinoReferencia: String?,
    val adjuntoPdfNombre: String?,
    val requiereFirmaFisica: Boolean,
    val autorId: String,
    val autorNombre: String,
    val fechaHora: String
)

/** Resultado del despacho a UN apoderado (RF-08) por un canal (RF-11). INMUTABLE (RN-02). */
@Entity(
    tableName = "envios",
    foreignKeys = [
        ForeignKey(
            entity = ComunicacionEntity::class,
            parentColumns = ["id"],
            childColumns = ["comunicacionId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = ApoderadoEntity::class,
            parentColumns = ["id"],
            childColumns = ["apoderadoId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("comunicacionId"), Index("apoderadoId")]
)
data class EnvioEntity(
    @PrimaryKey val id: String,
    val comunicacionId: String,
    val apoderadoId: String,
    val canal: String,
    val estado: String,
    val detalle: String
)
