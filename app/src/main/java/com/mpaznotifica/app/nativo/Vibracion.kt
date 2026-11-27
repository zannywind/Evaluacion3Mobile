package com.mpaznotifica.app.nativo

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * RECURSO NATIVO: vibración corta que acompaña la alerta visual del resultado de un envío.
 * Requiere el permiso VIBRATE (normal, sin solicitud en tiempo de ejecución).
 */
object Vibracion {

    @Suppress("DEPRECATION")
    fun confirmar(context: Context, exito: Boolean = true) {
        try {
            val vibrador = obtenerVibrador(context) ?: return
            if (!vibrador.hasVibrator()) return
            // Un pulso breve si todo salió bien; dos pulsos si hubo fallidos.
            val patron = if (exito) longArrayOf(0, 120) else longArrayOf(0, 120, 90, 120)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrador.vibrate(VibrationEffect.createWaveform(patron, -1))
            } else {
                vibrador.vibrate(patron, -1)
            }
        } catch (e: Exception) {
            // Si el dispositivo no puede vibrar, la alerta visual igual se muestra.
        }
    }

    @Suppress("DEPRECATION")
    private fun obtenerVibrador(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
}
