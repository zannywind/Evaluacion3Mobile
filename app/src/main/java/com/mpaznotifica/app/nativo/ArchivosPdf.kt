package com.mpaznotifica.app.nativo

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.mpaznotifica.app.domain.ReglasComunicado
import com.mpaznotifica.app.model.AdjuntoPdf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * RECURSO NATIVO: selector de archivos del sistema (Storage Access Framework). El usuario elige
 * el PDF con el selector nativo (sin permisos de almacenamiento); aquí se lee su nombre, tipo y
 * tamaño para validarlo, y al enviar se guarda una copia en el almacenamiento interno de la app.
 */
object ArchivosPdf {

    /** Resultado de inspeccionar el archivo elegido: el adjunto listo o el motivo del rechazo. */
    data class Inspeccion(val adjunto: AdjuntoPdf?, val error: String?)

    fun inspeccionar(context: Context, uri: Uri): Inspeccion {
        val tipo = context.contentResolver.getType(uri)
        try {
            // Mantiene el acceso de lectura hasta enviar el comunicado, aunque la app se recree.
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: Exception) {
            // El proveedor no ofrece permiso persistente: queda el permiso temporal de la sesión.
        }
        var nombre = "documento.pdf"
        var tamano: Long? = null
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnaNombre = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (columnaNombre >= 0) {
                        cursor.getString(columnaNombre)?.let { nombre = it }
                    }
                    val columnaTamano = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (columnaTamano >= 0 && !cursor.isNull(columnaTamano)) {
                        tamano = cursor.getLong(columnaTamano)
                    }
                }
            }
        } catch (e: Exception) {
            return Inspeccion(null, "No se pudo leer el archivo seleccionado")
        }
        val error = ReglasComunicado.errorAdjunto(tipo, tamano)
        return if (error != null) {
            Inspeccion(null, error)
        } else {
            Inspeccion(AdjuntoPdf(nombre, tamano ?: 0L, uri.toString()), null)
        }
    }

    /**
     * Copia el PDF a filesDir/adjuntos con un prefijo único del comunicado. Devuelve el nombre
     * visible del archivo, o null si no se pudo leer o copiar.
     */
    suspend fun guardarCopia(context: Context, adjunto: AdjuntoPdf, comunicacionId: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val carpeta = File(context.filesDir, "adjuntos").apply { mkdirs() }
                val nombreSeguro = File(adjunto.nombre).name.ifBlank { "documento.pdf" }
                val destino = File(carpeta, "${comunicacionId}_$nombreSeguro")
                val entrada = context.contentResolver.openInputStream(Uri.parse(adjunto.uri))
                if (entrada == null) {
                    null
                } else {
                    entrada.use { origen -> destino.outputStream().use { salida -> origen.copyTo(salida) } }
                    adjunto.nombre
                }
            } catch (e: Exception) {
                null
            }
        }
}
