package com.mpaznotifica.app.nativo

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mpaznotifica.app.MainActivity
import com.mpaznotifica.app.R
import com.mpaznotifica.app.model.ResumenEnvio

/**
 * RECURSO NATIVO: notificación local con el resultado del envío (cuántos destinatarios quedaron
 * despachados y cuántos fallidos). Al tocarla se abre la app. En Android 13+ requiere el permiso
 * POST_NOTIFICATIONS, que se solicita en tiempo de ejecución antes de enviar; si el usuario lo
 * rechaza, la app sigue funcionando con la alerta en pantalla. No usa cámara, micrófono, GPS ni
 * biometría (RNF-03).
 */
class Notificador(private val context: Context) {

    companion object {
        const val CANAL_ID = "envios_mpaz"
        const val PERMISO = Manifest.permission.POST_NOTIFICATIONS
    }

    fun requierePermiso(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun tienePermiso(): Boolean =
        !requierePermiso() ||
            ContextCompat.checkSelfPermission(context, PERMISO) == PackageManager.PERMISSION_GRANTED

    private fun crearCanal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(CANAL_ID, "Estado de envíos", NotificationManager.IMPORTANCE_DEFAULT)
            canal.description = "Resultado del despacho de comunicados"
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
        }
    }

    fun notificarEnvio(asunto: String, resumen: ResumenEnvio) {
        if (!tienePermiso()) return
        crearCanal()
        val texto = buildString {
            append("${resumen.despachados} despachado(s)")
            if (resumen.fallidos > 0) append(" · ${resumen.fallidos} fallido(s)")
            if (resumen.requiereFirmaFisica) append(" · entregar la firma en papel")
        }
        val intencion = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendiente = PendingIntent.getActivity(
            context,
            0,
            intencion,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notificacion = NotificationCompat.Builder(context, CANAL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Comunicado enviado: $asunto")
            .setContentText(texto)
            .setContentIntent(pendiente)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notificacion)
        } catch (e: SecurityException) {
            // El usuario revocó el permiso mientras se enviaba: no se muestra la notificación.
        }
    }
}
