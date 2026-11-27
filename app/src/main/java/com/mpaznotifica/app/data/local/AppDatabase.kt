package com.mpaznotifica.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Base de datos única de MPAZ Notifica (Room + SQLite). Contiene los datos de referencia
 * (usuarios, apoderados, estudiantes, talleres) y la bitácora (comunicaciones y envíos).
 *
 * Se usa con dos archivos distintos y sin relación entre sí: la CACHÉ de lo que entrega la API
 * (backend/), cuya fuente de verdad es el servidor, y la base propia del MODO LOCAL
 * (obtenerLocal), que funciona sin servidor.
 *
 * Versión 1. Si más adelante se cambian las entidades, subir la versión: mientras se desarrolla,
 * fallbackToDestructiveMigration() recrea la base (y la vuelve a sembrar) en lugar de fallar.
 */
@Database(
    entities = [
        UsuarioEntity::class,
        ApoderadoEntity::class,
        EstudianteEntity::class,
        TallerEntity::class,
        EstudianteTallerEntity::class,
        ComunicacionEntity::class,
        EnvioEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun apoderadoDao(): ApoderadoDao
    abstract fun estudianteDao(): EstudianteDao
    abstract fun comunicacionDao(): ComunicacionDao

    companion object {
        /** Caché de lo que entrega el servidor (modo servidor). */
        private const val NOMBRE = "mpaz_notifica.db"

        /** Datos propios del modo local: otro archivo, independiente de la caché del servidor. */
        const val NOMBRE_LOCAL = "mpaz_local.db"

        private val instancias = HashMap<String, AppDatabase>()

        private val TABLAS_PROTEGIDAS = listOf("comunicaciones", "envios")

        /** Crea (si no existen) los disparadores que rechazan UPDATE y DELETE sobre la bitácora. */
        fun crearProteccion(db: SupportSQLiteDatabase) {
            for (tabla in TABLAS_PROTEGIDAS) {
                db.execSQL(
                    "CREATE TRIGGER IF NOT EXISTS ${tabla}_inmutable_update BEFORE UPDATE ON $tabla " +
                        "BEGIN SELECT RAISE(ABORT, 'RN-02: el registro enviado es inmutable'); END"
                )
                db.execSQL(
                    "CREATE TRIGGER IF NOT EXISTS ${tabla}_inmutable_delete BEFORE DELETE ON $tabla " +
                        "BEGIN SELECT RAISE(ABORT, 'RN-02: el registro enviado es inmutable'); END"
                )
            }
        }

        /**
         * Solo para espejar la caché con el servidor (RoomCache.reemplazarBitacora, dentro de una
         * transacción): quita los disparadores de DELETE. UPDATE sigue bloqueado siempre.
         */
        fun quitarProteccionBorrado(db: SupportSQLiteDatabase) {
            for (tabla in TABLAS_PROTEGIDAS) {
                db.execSQL("DROP TRIGGER IF EXISTS ${tabla}_inmutable_delete")
            }
        }

        /** Instancia única por archivo (patrón Singleton): la caché del servidor o, con [NOMBRE_LOCAL], la local. */
        fun obtener(context: Context, nombre: String = NOMBRE): AppDatabase =
            synchronized(instancias) {
                instancias.getOrPut(nombre) {
                    Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, nombre)
                        .addCallback(ProteccionBitacora)
                        .fallbackToDestructiveMigration()
                        .build()
                }
            }

        /** Base del modo local (independiente de la caché del servidor). */
        fun obtenerLocal(context: Context): AppDatabase = obtener(context, NOMBRE_LOCAL)
    }

    /**
     * RN-02: la bitácora es inmutable. Además de no ofrecer @Update/@Delete en uso, la base
     * rechaza cualquier UPDATE o DELETE sobre comunicaciones y envíos mediante disparadores.
     */
    private object ProteccionBitacora : RoomDatabase.Callback() {
        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            AppDatabase.crearProteccion(db)
        }
    }
}
